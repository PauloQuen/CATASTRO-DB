import java.sql.*;
import java.util.Scanner;

public class pbm_tributo_presupuestoCRUD {

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
            System.out.println("\n===== CRUD ASOCIATIVO TRIBUTO-PRESUPUESTO (PBM_TRIBUTO_PRESUPUESTO) =====");
            System.out.println("1. Vincular Tributo a Presupuesto");
            System.out.println("2. Listar Vínculos Activos (Con INNER JOINs)");
            System.out.println("3. Buscar Vínculo por Clave Compuesta");
            System.out.println("4. Actualizar Proyección Estimada");
            System.out.println("5. Eliminar Vínculo (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarTributoPresupuesto(); break;
                case 2: listarTributoPresupuesto(); break;
                case 3: buscarTributoPresupuesto(); break;
                case 4: actualizarTributoPresupuesto(); break;
                case 5: eliminarTributoPresupuesto(); break;
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

    public static void insertarTributoPresupuesto() {
        Connection conn = conectar();
        try {
            System.out.print("Código Presupuesto Anual FK (TriPrePre - Int): ");
            int triPrePre = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial verificando la existencia del marco presupuestal anual.
            if (!existeId(conn, "pbm_presupuesto_anual", "PreCod", triPrePre)) {
                System.out.println("Error: El código de presupuesto indicado no existe."); return;
            }

            System.out.print("Código Tributario Cabecera FK (TriPreTri - Int): ");
            int triPreTri = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial garantizando que la cartera de tributos asociada sea válida.
            if (!existeId(conn, "pat_tributo_cab", "TriCabCod", triPreTri)) {
                System.out.println("Error: El código de tributo indicado no existe."); return;
            }

            System.out.print("Monto de Ingreso Proyectado S/: ");
            double proy = sc.nextDouble();

            System.out.print("Monto Total Efectivamente Recaudado S/: ");
            double reca = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO pbm_tributo_presupuesto (TriPrePre, TriPreTri, TriPreProy, TriPreReca, TriPreEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, triPrePre);
            stmt.setInt(2, triPreTri);
            stmt.setDouble(3, proy);
            stmt.setDouble(4, reca);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("Relación Tributo-Presupuesto enlazada con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarTributoPresupuesto() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN múltiple para mostrar los años fiscales y conceptos de recaudación en lugar de códigos fríos.
            String sql = "SELECT tp.TriPrePre, tp.TriPreTri, p.PreAno, t.TriCabNom, tp.TriPreProy, tp.TriPreReca " +
                         "FROM pbm_tributo_presupuesto tp " +
                         "INNER JOIN pbm_presupuesto_anual p ON tp.TriPrePre = p.PreCod " +
                         "INNER JOIN pat_tributo_cab t ON tp.TriPreTri = t.TriCabCod " +
                         "WHERE tp.TriPreEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- MAPEO DE INGRESOS TRIBUTARIOS POR PRESUPUESTO ---");
            while (rs.next()) {
                System.out.println(
                    "Presupuesto ID: " + rs.getInt("TriPrePre") + " (Año: " + rs.getInt("PreAno") + ") | " +
                    "Tributo ID: " + rs.getInt("TriPreTri") + " (" + rs.getString("TriCabNom") + ") | " +
                    "Proyectado: S/ " + rs.getDouble("TriPreProy") + " | " +
                    "Recaudado: S/ " + rs.getDouble("TriPreReca")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarTributoPresupuesto() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Realiza una búsqueda exacta usando la clave compuesta para ubicar la asignación tributaria.
            String sql = "SELECT tp.*, p.PreAno, t.TriCabNom FROM pbm_tributo_presupuesto tp " +
                         "INNER JOIN pbm_presupuesto_anual p ON tp.TriPrePre = p.PreCod " +
                         "INNER JOIN pat_tributo_cab t ON tp.TriPreTri = t.TriCabCod " +
                         "WHERE tp.TriPrePre = ? AND tp.TriPreTri = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código Presupuesto Matriz: ");
            int preCod = sc.nextInt();
            System.out.print("Código Tributo Matriz: ");
            int triCod = sc.nextInt();

            stmt.setInt(1, preCod);
            stmt.setInt(2, triCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- FICHA DE ASIGNACIÓN FINANCIERA ---");
                System.out.println("Presupuesto Operativo : ID " + rs.getInt("TriPrePre") + " (Año Fiscal: " + rs.getInt("PreAno") + ")");
                System.out.println("Tributo Gravado        : ID " + rs.getInt("TriPreTri") + " (" + rs.getString("TriCabNom") + ")");
                System.out.println("Monto Proyectado       : S/ " + rs.getDouble("TriPreProy"));
                System.out.println("Monto Recaudado Real   : S/ " + rs.getDouble("TriPreReca"));
                System.out.println("Estado Ficha           : " + rs.getString("TriPreEstReg"));
            } else {
                System.out.println("Vínculo tributario no encontrado en el marco presupuestal.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarTributoPresupuesto() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Actualiza las metas de proyección económica identificando el registro por su clave primaria compuesta.
            String sql = "UPDATE pbm_tributo_presupuesto SET TriPreProy=?, TriPreReca=? WHERE TriPrePre=? AND TriPreTri=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Presupuesto Matriz a modificar: ");
            int preCod = sc.nextInt();
            System.out.print("Código Tributo Matriz a modificar: ");
            int triCod = sc.nextInt();

            System.out.print("Nuevo Monto Proyectado S/: ");
            double nProy = sc.nextDouble();
            System.out.print("Nuevo Monto Recaudado S/: ");
            double nReca = sc.nextDouble();

            stmt.setDouble(1, nProy);
            stmt.setDouble(2, nReca);
            stmt.setInt(3, preCod);
            stmt.setInt(4, triCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Metas financieras de la relación presupuestal actualizadas.");
            else System.out.println("No se encontró la combinación de registros indicada.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarTributoPresupuesto() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Desvincula físicamente la relación asociativa utilizando la clave compuesta.
            String sql = "DELETE FROM pbm_tributo_presupuesto WHERE TriPrePre=? AND TriPreTri=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Presupuesto Matriz a desvincular: ");
            int preCod = sc.nextInt();
            System.out.print("Código Tributo Matriz a desvincular: ");
            int triCod = sc.nextInt();

            stmt.setInt(1, preCod);
            stmt.setInt(2, triCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Vínculo financiero purgado de la base de datos.");
            else System.out.println("No se localizó el registro indicado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}