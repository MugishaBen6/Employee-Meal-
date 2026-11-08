package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.PageResponse;
import com.emeal.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Recycle Bin. ADMIN only: view, restore and permanently delete. */
@RestController
@RequestMapping("/trash")
@PreAuthorize("hasRole('ADMIN')")
public class TrashController {

    private final TrashService trashService;

    public TrashController(TrashService trashService) {
        this.trashService = trashService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TrashService.TrashItemDTO>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Trash items", trashService.list(type, query, page, size)));
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Long>>> ids(@RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success("Trash item ids", trashService.ids(type, query)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Trash summary", trashService.summary()));
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restore(@RequestBody List<Long> ids) {
        Map<String, Object> result = trashService.restore(ids);
        return ResponseEntity.ok(ApiResponse.success("Restored " + result.get("restored") + " item(s)"
                + (((Integer) result.get("failed")) > 0 ? ", " + result.get("failed") + " could not be restored" : ""), result));
    }

    /** Irreversible. The client must send confirm=DELETE. */
    @PostMapping("/purge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purge(@RequestBody List<Long> ids,
                                                                  @RequestParam String confirm) {
        if (!"DELETE".equals(confirm)) {
            throw new com.emeal.exception.BadRequestException("Type DELETE to confirm permanent deletion.");
        }
        int purged = trashService.purge(ids);
        return ResponseEntity.ok(ApiResponse.success("Permanently deleted " + purged + " item(s)", Map.of("purged", purged)));
    }
}
