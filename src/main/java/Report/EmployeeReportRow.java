package Report;

/*
Fila "plana" para el reporte de Funcionarios (src/main/resources/reports/funcionarios.jrxml).
JasperReports enlaza cada campo $F{...} del .jrxml con un getter de este
bean por convencion de JavaBeans (getId, getName, getPhoneNumber), asi
que los nombres de estos metodos deben calzar exactamente con los
nombres de los <field> del .jrxml.
*/
public class EmployeeReportRow {

    private final int id;
    private final String name;
    private final int phoneNumber;

    public EmployeeReportRow(int id, String name, int phoneNumber) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }
}
