import java.sql.*;
import java.util.Scanner;

public class pct_reportes_cabCRUD {

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
            System.out.println("\n===== CRUD CABECERA DE REPORTES TRANSACCIONALES (PCT_REPORTES_CAB) =====");
            System.out.println("1. Registrar Nueva Orden de Reporte");
            System.out.println("2. Listar Reportes Activos (Con INNER JOINs)");
            System.out.println("3. Buscar Reporte por Código");
            System.out.println("4. Actualizar Descripción / Glosa");
            System.out.println("5. Eliminar Orden de Reporte");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarReporteCab(); break;
                case 2: listarReporteCab(); break;
                case 3: buscarReporteCab(); break;
                case 4: actualizarReporteCab(); break;
                case 5: eliminarReporteCab(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Método genérico para validar la existencia de llaves foráneas en tablas referenciales o maestros.
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

    public static void insertarReporteCab() {
        Connection conn = conectar();
        try {
            System.out.print("Código de Reporte Transaccional (RepCabCod - Int): ");
            int repCabCod = sc.nextInt();

            System.out.print("Código Presupuesto Anual FK (FKRepCabPre - Int): ");
            int fkRepCabPre = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial confirmando que la orden de fiscalización esté amparada bajo un techo presupuestal anual válido.
            if (!existeId(conn, "pbm_presupuesto_anual", "PreCod", fkRepCabPre)) {
                System.out.println("Error: El código de Presupuesto Anual indicado no existe."); return;
            }

            System.out.print("Código Municipalidad FK (FKRepCabMun - Int): ");
            int fkRepCabMun = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial garantizando que la municipalidad ejecutora del reporte esté registrada en el sistema.
            if (!existeId(conn, "c1m_municipalidad", "MunCod", fkRepCabMun)) {
                System.out.println("Error: La Municipalidad indicada no existe."); return;
            }
            sc.nextLine(); // Limpieza buffer

            System.out.print("Descripción / Glosa Operativa de la Orden: ");
            String desc = sc.nextLine();

            String sql = "INSERT INTO pct_reportes_cab (RepCabCod, FKRepCabPre, FKRepCabMun, RepCabDes, RepCabEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, repCabCod);
            stmt.setInt(2, fkRepCabPre);
            stmt.setInt(3, fkRepCabMun);
            stmt.setString(4, desc);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("✔ Cabecera de reporte transaccional creada con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarReporteCab() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN múltiple para decodificar los códigos fríos en los nombres institucionales de los municipios y años fiscales.
            String sql = "SELECT rc.RepCabCod, rc.RepCabDes, p.PreAno, m.MunNom " +
                         "FROM pct_reportes_cab rc " +
                         "INNER JOIN pbm_presupuesto_anual p ON rc.FKRepCabPre = p.PreCod " +
                         "INNER JOIN c1m_municipalidad m ON rc.FKRepCabMun = m.MunCod " +
                         "WHERE rc.RepCabEstReg = '1' ORDER BY rc.RepCabCod ASC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE ÓRDENES DE REPORTES ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod Reporte: " + rs.getInt("RepCabCod") + " | " +
                    "Municipio: " + rs.getString("MunNom") + " | " +
                    "Año Marco: " + rs.getInt("PreAno") + " | " +
                    "Descripción: " + rs.getString("RepCabDes")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarReporteCab() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Recupera la orden de reporte cruzando las descripciones completas de sus llaves foráneas.
            String sql = "SELECT rc.*, p.PreAno, m.MunNom FROM pct_reportes_cab rc " +
                         "INNER JOIN pbm_presupuesto_anual p ON rc.FKRepCabPre = p.PreCod " +
                         "INNER JOIN c1m_municipalidad m ON rc.FKRepCabMun = m.MunCod " +
                         "WHERE rc.RepCabCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código de Reporte a consultar: ");
            int repCabCod = sc.nextInt();
            stmt.setInt(1, repCabCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE DE ORDEN DE REPORTE ---");
                System.out.println("Código Operativo  : " + rs.getInt("RepCabCod"));
                System.out.println("Municipalidad     : " + rs.getString("MunNom"));
                System.out.println("Año Presupuestal  : " + rs.getInt("PreAno"));
                System.out.println("Glosa Informativa : " + rs.getString("RepCabDes"));
                System.out.println("Estado del Registro: " + rs.getString("RepCabEstReg"));
            } else {
                System.out.println("Orden de reporte transaccional no encontrada.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarReporteCab() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Permite corregir la glosa informativa o de destino de la orden operativa localizándola por su ID primario.
            String sql = "UPDATE pct_reportes_cab SET RepCabDes=? WHERE RepCabCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Reporte a modificar: ");
            int repCabCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Ingrese la Nueva Descripción/Destino Operativo: ");
            String nuevaDesc = sc.nextLine();

            stmt.setString(1, nuevaDesc);
            stmt.setInt(2, repCabCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("✔ Glosa de la orden transaccional actualizada.");
            else System.out.println("No se encontró el registro operativo solicitado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarReporteCab() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Remueve físicamente el registro cabecera de la orden operativa seleccionada.
            String sql = "DELETE FROM pct_reportes_cab WHERE RepCabCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Reporte a eliminar físicamente: ");
            int repCabCod = sc.nextInt();
            stmt.setInt(1, repCabCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("✔ Orden de reporte purgada físicamente del sistema operativo.");
            else System.out.println("No se localizó el registro indicado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            // EXPLICACIÓN: El motor relacional restringe el borrado si ya existen líneas de desgloses vinculadas en el detalle.
            System.out.println("ERROR: No se puede eliminar la cabecera porque contiene líneas de detalles asociadas en plena ejecución.");
        }
    }
}
