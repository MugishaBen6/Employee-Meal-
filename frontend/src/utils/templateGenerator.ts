/**
 * Utility to generate and download Excel template directly on the client side
 * as a robust fallback if backend API is offline or deploying.
 */

export function downloadClientExcelTemplate(): void {
  // XML Spreadsheet 2003 format - natively supported and styled by Microsoft Excel & Apple Numbers
  const xmlContent = `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Title>Employee Import Template</Title>
  <Author>Employee Meal Management System</Author>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Borders/>
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#000000"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="Header">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#4338CA" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="Sample">
   <Alignment ss:Vertical="Center"/>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#475569" ss:Italic="1"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Employees">
  <Table ss:DefaultRowHeight="20">
   <Column ss:Width="160"/>
   <Column ss:Width="120"/>
   <Column ss:Width="130"/>
   <Column ss:Width="110"/>
   <Column ss:Width="110"/>
   <Row ss:Height="26" ss:StyleID="Header">
    <Cell><Data ss:Type="String">Employee Name</Data></Cell>
    <Cell><Data ss:Type="String">Telephone</Data></Cell>
    <Cell><Data ss:Type="String">Position</Data></Cell>
    <Cell><Data ss:Type="String">Meal Status</Data></Cell>
    <Cell><Data ss:Type="String">Amount Used</Data></Cell>
   </Row>
   <Row ss:StyleID="Sample">
    <Cell><Data ss:Type="String">John Doe</Data></Cell>
    <Cell><Data ss:Type="String">0781234567</Data></Cell>
    <Cell><Data ss:Type="String">Worker</Data></Cell>
    <Cell><Data ss:Type="String">Ate</Data></Cell>
    <Cell><Data ss:Type="Number">1500</Data></Cell>
   </Row>
   <Row ss:StyleID="Sample">
    <Cell><Data ss:Type="String">Jane Smith</Data></Cell>
    <Cell><Data ss:Type="String">0791234567</Data></Cell>
    <Cell><Data ss:Type="String">Manager</Data></Cell>
    <Cell><Data ss:Type="String">Not Ate</Data></Cell>
    <Cell><Data ss:Type="Number">0</Data></Cell>
   </Row>
   <Row ss:StyleID="Sample">
    <Cell><Data ss:Type="String">Eric Mugisha</Data></Cell>
    <Cell><Data ss:Type="String">0787654321</Data></Cell>
    <Cell><Data ss:Type="String">Driver</Data></Cell>
    <Cell><Data ss:Type="String">Ate</Data></Cell>
    <Cell><Data ss:Type="Number">1500</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
</Workbook>`;

  const blob = new Blob([xmlContent], {
    type: 'application/vnd.ms-excel;charset=utf-8',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'employee_import_template.xls');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export function downloadClientCsvTemplate(): void {
  const csvContent =
    '\uFEFF' +
    'Employee Name,Telephone,Position,Meal Status,Amount Used\r\n' +
    'John Doe,0781234567,Worker,Ate,1500\r\n' +
    'Jane Smith,0791234567,Manager,Not Ate,0\r\n' +
    'Eric Mugisha,0787654321,Driver,Ate,1500\r\n';

  const blob = new Blob([csvContent], {
    type: 'text/csv;charset=utf-8;',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'employee_import_template.csv');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
