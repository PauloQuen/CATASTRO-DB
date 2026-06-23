import java.sql.*;
import java.util.Scanner;

public class c3m_viviendaCRUD {

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
            System.out.println("\n===== CRUD VIVIENDA (SISTEMA INTEGRADO) =====");
            System.out.println("1. Insertar");
            System.out.println("2. Listar (Con INNER JOINs)");
            System.out.println("3. Buscar");
            System.out.println("4. Actualizar");
            System.out.println("5. Eliminar");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarVivienda(); break;
                case 2: listarVivienda(); break;
                case 3: buscarVivienda(); break;
                case 4: actualizarVivienda(); break;
                case 5: eliminarVivienda(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Método genérico para validar la existencia de llaves foráneas en tablas referenciales
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

    public static void insertarVivienda() {
        Connection conn = conectar();
        try {
            System.out.print("Codigo Vivienda: ");
            String cod = sc.nextLine();

            System.out.print("Zona: ");
            int zona = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial con la tabla de zonas
            if (!existeId(conn, "c2m_zona", "ZonCod", zona)) {
                System.out.println("Error: Zona no válida."); return;
            }

            System.out.print("Direccion: ");
            int dir = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial con la tabla de direcciones
            if (!existeId(conn, "c3m_direccion", "DirCod", dir)) {
                System.out.println("Error: Dirección no válida."); return;
            }
            sc.nextLine(); // Limpieza buffer

            System.out.print("Ubigeo: ");
            String ubigeo = sc.nextLine();

            System.out.print("Tipo Predio: ");
            String tipo = sc.nextLine();
            // EXPLICACIÓN: Valida integridad referencial con tipos de predio
            if (!existeId(conn, "c5m_tipo_predio", "TpPrCod", tipo)) {
                System.out.println("Error: Tipo de predio no válido."); return;
            }

            System.out.print("Uso Predio: ");
            String uso = sc.nextLine();
            // EXPLICACIÓN: Valida integridad referencial con usos de predio
            if (!existeId(conn, "c5m_uso_predio", "UsPrCod", uso)) {
                System.out.println("Error: Uso de predio no válido."); return;
            }

            System.out.print("Valor Catastral: ");
            double valor = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO C3M_VIVIENDA (VivCod,VivZon,VivDir,VivUbigeo,VivTipPr,VivUsoPr,VivVal,VivEstReg) VALUES (?,?,?,?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cod);
            stmt.setInt(2, zona);
            stmt.setInt(3, dir);
            stmt.setString(4, ubigeo);
            stmt.setString(5, tipo);
            stmt.setString(6, uso);
            stmt.setDouble(7, valor);
            stmt.setString(8, "1");

            stmt.executeUpdate();
            System.out.println("✔ Vivienda registrada con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarVivienda() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN múltiple para mostrar nombres y calles en lugar de códigos fríos
            String sql = "SELECT v.VivCod, v.VivUbigeo, d.DirViaNom, d.DirNum, tp.TpPrNom, up.UsPrNom, v.VivVal " +
                         "FROM C3M_VIVIENDA v " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "INNER JOIN c5m_tipo_predio tp ON v.VivTipPr = tp.TpPrCod " +
                         "INNER JOIN c5m_uso_predio up ON v.VivUsoPr = up.UsPrCod " +
                         "WHERE v.VivEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE PREDIOS ACTIVOS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod: " + rs.getString("VivCod") + " | " +
                    "Calle: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " | " +
                    "Tipo: " + rs.getString("TpPrNom") + " | " +
                    "Uso: " + rs.getString("UsPrNom") + " | " +
                    "Valor: S/ " + rs.getDouble("VivVal")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarVivienda() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Trae la información del predio cruzada con los nombres descriptivos de sus foráneas
            String sql = "SELECT v.*, z.ZonNom, d.DirViaNom, tp.TpPrNom, up.UsPrNom FROM C3M_VIVIENDA v " +
                         "INNER JOIN c2m_zona z ON v.VivZon = z.ZonCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "INNER JOIN c5m_tipo_predio tp ON v.VivTipPr = tp.TpPrCod " +
                         "INNER JOIN c5m_uso_predio up ON v.VivUsoPr = up.UsPrCod " +
                         "WHERE v.VivCod=?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Codigo Vivienda a buscar: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- DATOS DEL PREDIO ---");
                System.out.println("Código: " + rs.getString("VivCod"));
                System.out.println("Zona: " + rs.getString("ZonNom"));
                System.out.println("Dirección Calle: " + rs.getString("DirViaNom"));
                System.out.println("Ubigeo Catastral: " + rs.getString("VivUbigeo"));
                System.out.println("Tipo Predio: " + rs.getString("TpPrNom"));
                System.out.println("Uso Predio: " + rs.getString("UsPrNom"));
                System.out.println("Valor de Autovalúo: S/ " + rs.getDouble("VivVal"));
            } else {
                System.out.println("Vivienda no encontrada.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarVivienda() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE C3M_VIVIENDA SET VivVal=? WHERE VivCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda a modificar: ");
            String codigo = sc.nextLine();

            System.out.print("Nuevo Valor Catastral: ");
            double valor = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            stmt.setDouble(1, valor);
            stmt.setString(2, codigo);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("✔ Valor catastral actualizado.");
            else System.out.println("No se encontró el predio.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarVivienda() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM C3M_VIVIENDA WHERE VivCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda a eliminar: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("✔ Registro eliminado físicamente.");
            else System.out.println("No se encontró la vivienda.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}