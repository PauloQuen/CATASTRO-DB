import java.sql.*;
import java.util.Scanner;

public class pbm_presupuesto_anualCRUD {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db";
    static final String USER = "root";
    static final String PASSWORD = "";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        menuPrincipal();
    }

    public static Connection conectar() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Error conexión: " + e.getMessage());
        }
        return conn;
    }

    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD PRESUPUESTO ANUAL INSTITUCIONAL (PBM_PRESUPUESTO_ANUAL) =====");
            System.out.println("1. Registrar Presupuesto Institucional");
            System.out.println("2. Listar Presupuestos (Con INNER JOIN)");
            System.out.println("3. Buscar por Código Maestro");
            System.out.println("4. Actualizar Asignación Financiera");
            System.out.println("5. Eliminar Registro Presupuestal");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarPresupuestoAnual(); break;
                case 2: listarPresupuestoAnual(); break;
                case 3: buscarPresupuestoAnual(); break;
                case 4: actualizarPresupuestoAnual(); break;
                case 5: eliminarPresupuestoAnual(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Método genérico para validar la existencia de llaves foráneas en tablas referenciales.
    private static boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) stmt.setInt(1, (Integer) id);
            else stmt.setString(1, (String) id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarPresupuestoAnual() {
        Connection conn = conectar();
        try {
            System.out.print("Código de Matriz Presupuestal (PreCod - Int): ");
            int preCod = sc.nextInt();

            System.out.print("Código Municipalidad FK (FKPreMun - Int): ");
            int fkPreMun = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial impidiendo asignar un presupuesto a un municipio que no existe.
            if (!existeId(conn, "c1m_municipalidad", "MunCod", fkPreMun)) {
                System.out.println("Error: La Municipalidad indicada no está dada de alta."); return;
            }

            System.out.print("Año Fiscal Presupuestado (PreAno - Int): ");
            int preAno = sc.nextInt();

            System.out.print("Monto Inicial Aprobado S/: ");
            double preMonto = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO pbm_presupuesto_anual (PreCod, FKPreMun, PreAno, PreMonto, PreEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, preCod);
            stmt.setInt(2, fkPreMun);
            stmt.setInt(3, preAno);
            stmt.setDouble(4, preMonto);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("Presupuesto anual institucional guardado con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarPresupuestoAnual() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN para resolver el código municipal en el nombre institucional de la alcaldía corporativa.
            String sql = "SELECT p.PreCod, p.PreAno, p.PreMonto, m.MunNom " +
                         "FROM pbm_presupuesto_anual p " +
                         "INNER JOIN c1m_municipalidad m ON p.FKPreMun = m.MunCod " +
                         "WHERE p.PreEstReg = '1' ORDER BY p.PreAno DESC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- MARCOS PRESUPUESTALES INSTITUCIONALES ---");
            while (rs.next()) {
                System.out.println(
                    "ID Presupuesto: " + rs.getInt("PreCod") + " | " +
                    "Entidad: " + rs.getString("MunNom") + " | " +
                    "Año Fiscal: " + rs.getInt("PreAno") + " | " +
                    "Monto Techo: S/ " + rs.getDouble("PreMonto")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarPresupuestoAnual() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Trae la ficha del marco presupuestal vinculando la descripción de la municipalidad.
            String sql = "SELECT p.*, m.MunNom FROM pbm_presupuesto_anual p " +
                         "INNER JOIN c1m_municipalidad m ON p.FKPreMun = m.MunCod " +
                         "WHERE p.PreCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese Código de Presupuesto a buscar: ");
            int preCod = sc.nextInt();
            stmt.setInt(1, preCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE DE PRESUPUESTO MAESTRO ---");
                System.out.println("Código Registro  : " + rs.getInt("PreCod"));
                System.out.println("Municipalidad    : " + rs.getString("MunNom") + " (ID: " + rs.getInt("FKPreMun") + ")");
                System.out.println("Periodo Fiscal   : " + rs.getInt("PreAno"));
                System.out.println("Asignación Techo : S/ " + rs.getDouble("PreMonto"));
                System.out.println("Estado Ficha     : " + rs.getString("PreEstReg"));
            } else {
                System.out.println("Presupuesto no encontrado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarPresupuestoAnual() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Modifica los techos económicos del plan anual local localizando el registro por su ID maestro.
            String sql = "UPDATE pbm_presupuesto_anual SET PreMonto=? WHERE PreCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Presupuesto a rectificar: ");
            int preCod = sc.nextInt();

            System.out.print("Nuevo Monto Asignado S/: ");
            double nuevoMonto = sc.nextDouble();

            stmt.setDouble(1, nuevoMonto);
            stmt.setInt(2, preCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Marco financiero modificado correctamente.");
            else System.out.println("No se localizó la ficha del presupuesto.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarPresupuestoAnual() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Borra físicamente la entidad referencial del presupuesto anual seleccionado.
            String sql = "DELETE FROM pbm_presupuesto_anual WHERE PreCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Presupuesto a purgar físicamente: ");
            int preCod = sc.nextInt();
            stmt.setInt(1, preCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Registro presupuestal eliminado de la base de datos.");
            else System.out.println("No se encontró el registro.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            // EXPLICACIÓN: El manejador SQL bloquea el borrado si ya existen reportes mensuales transaccionales asociados a este marco anual.
            System.out.println("ERROR: No se puede eliminar el presupuesto debido a restricciones de integridad con la tabla de reportes.");
        }
    }
}
