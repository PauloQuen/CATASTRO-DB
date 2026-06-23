import java.sql.*;
import java.util.Scanner;

public class c3m_casa_particularCRUD {

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
            System.out.println("\n===== CRUD CASA PARTICULAR (SISTEMA INTEGRADO) =====");
            System.out.println("1. Insertar Casa Particular");
            System.out.println("2. Listar Casas Particulares");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Características");
            System.out.println("5. Eliminar Registro");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarCasaParticular(); break;
                case 2: listarCasaParticular(); break;
                case 3: buscarCasaParticular(); break;
                case 4: actualizarCasaParticular(); break;
                case 5: eliminarCasaParticular(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para comprobar consistencia relacional previa
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

    public static void insertarCasaParticular() {
        Connection conn = conectar();
        try {
            System.out.print("Codigo Vivienda (Debe existir en C3M_VIVIENDA): ");
            String cpCod = sc.nextLine();

            // EXPLICACIÓN: Valida integridad referencial estricta (1:1 Herencia) con la tabla madre Vivienda
            if (!existeId(conn, "c3m_vivienda", "VivCod", cpCod)) {
                System.out.println("❌ Error: El código de vivienda especificado no existe en la base catastral.");
                return;
            }

            // EXPLICACIÓN: Evita la duplicidad en la tabla especializada
            if (existeId(conn, "c3m_casa_particular", "CpCod", cpCod)) {
                System.out.println("❌ Error: Este predio ya se encuentra registrado como una Casa Particular.");
                return;
            }

            System.out.print("Número de Pisos: ");
            int numPisos = sc.nextInt();
            
            System.out.print("Área Construida (m2): ");
            double areaConst = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Material Predominante (Noble/Adobe/Madera): ");
            String material = sc.nextLine();

            String sql = "INSERT INTO c3m_casa_particular (CpCod, CpNumPis, CpAreCons, CpMatPred, CpEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cpCod);
            stmt.setInt(2, numPisos);
            stmt.setDouble(3, areaConst);
            stmt.setString(4, material);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("✔ Características de Casa Particular añadidas correctamente al predio.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarCasaParticular() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN con la tabla madre y direcciones para generar un listado unificado y claro
            String sql = "SELECT cp.CpCod, d.DirViaNom, d.DirNum, cp.CpNumPis, cp.CpAreCons, v.VivVal " +
                         "FROM c3m_casa_particular cp " +
                         "INNER JOIN c3m_vivienda v ON cp.CpCod = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE cp.CpEstReg = '1' AND v.VivEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE CASAS PARTICULARES ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod: " + rs.getString("CpCod") + " | " +
                    "Ubicación: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " | " +
                    "Pisos: " + rs.getInt("CpNumPis") + " | " +
                    "Área: " + rs.getDouble("CpAreCons") + "m² | " +
                    "Autovalúo: S/ " + rs.getDouble("VivVal")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarCasaParticular() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Extrae los datos de la subclase acoplados con los descriptores geográficos y económicos de la madre
            String sql = "SELECT cp.*, d.DirViaNom, d.DirNum, v.VivUbigeo, v.VivVal, z.ZonNom " +
                         "FROM c3m_casa_particular cp " +
                         "INNER JOIN c3m_vivienda v ON cp.CpCod = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "INNER JOIN c2m_zona z ON v.VivZon = z.ZonCod " +
                         "WHERE cp.CpCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código de Casa Particular a buscar: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- FICHA TÉCNICA: CASA PARTICULAR ---");
                System.out.println("Código Catastral: " + rs.getString("CpCod"));
                System.out.println("Ubicación Física: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " (Zona: " + rs.getString("ZonNom") + ")");
                System.out.println("Ubigeo Territorial: " + rs.getString("VivUbigeo"));
                System.out.println("Número de Niveles: " + rs.getInt("CpNumPis"));
                System.out.println("Superficie Techada: " + rs.getDouble("CpAreCons") + " m2");
                System.out.println("Componente Estructural: " + rs.getString("CpMatPred"));
                System.out.println("Valoración de Autovalúo: S/ " + rs.getDouble("VivVal"));
            } else {
                System.out.println("Registro de Casa Particular no encontrado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarCasaParticular() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c3m_casa_particular SET CpNumPis = ?, CpAreCons = ?, CpMatPred = ? WHERE CpCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de la casa a modificar: ");
            String codigo = sc.nextLine();

            if (!existeId(conn, "c3m_casa_particular", "CpCod", codigo)) {
                System.out.println("❌ No se encontró la casa particular indicada.");
                return;
            }

            System.out.print("Nuevo Número de Pisos: ");
            int pisos = sc.nextInt();
            System.out.print("Nueva Área Construida (m2): ");
            double area = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Nuevo Material Predominante: ");
            String material = sc.nextLine();

            stmt.setInt(1, pisos);
            stmt.setDouble(2, area);
            stmt.setString(3, material);
            stmt.setString(4, codigo);

            stmt.executeUpdate();
            System.out.println("✔ Atributos físicos estructurales actualizados con éxito.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarCasaParticular() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM c3m_casa_particular WHERE CpCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de Casa Particular a eliminar: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("✔ Registro de Casa Particular eliminado físicamente.");
            } else {
                System.out.println("No se encontró el registro.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}