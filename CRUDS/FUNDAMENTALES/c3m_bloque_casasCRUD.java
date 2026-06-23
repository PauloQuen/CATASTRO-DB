import java.sql.*;
import java.util.Scanner;

public class c3m_bloque_casasCRUD {

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
            System.out.println("\n===== CRUD BLOQUE DE CASAS / EDIFICIOS =====");
            System.out.println("1. Insertar Bloque de Casas");
            System.out.println("2. Listar Bloques Activos");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Atributos");
            System.out.println("5. Eliminar Registro");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarBloqueCasas(); break;
                case 2: listarBloqueCasas(); break;
                case 3: buscarBloqueCasas(); break;
                case 4: actualizarBloqueCasas(); break;
                case 5: eliminarBloqueCasas(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para comprobar consistencia relacional previa en base de datos
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

    public static void insertarBloqueCasas() {
        Connection conn = conectar();
        try {
            System.out.print("Codigo Vivienda (Debe existir en C3M_VIVIENDA): ");
            String bloCod = sc.nextLine();

            // EXPLICACIÓN: Valida integridad relacional estricta (1:1 de herencia) con la tabla base C3M_VIVIENDA
            if (!existeId(conn, "c3m_vivienda", "VivCod", bloCod)) {
                System.out.println("Error: El código de predio madre no existe en C3M_VIVIENDA.");
                return;
            }

            // EXPLICACIÓN: Evita registros duplicados en esta especialización
            if (existeId(conn, "c3m_bloque_casas", "BloVivCod", bloCod)) {
                System.out.println("Error: Este predio ya está registrado como un Bloque de Casas.");
                return;
            }

            System.out.print("Metros Cuadrados Totales del Bloque (BloMetB): ");
            double metB = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Datos Adicionales / Observaciones (BloOd): ");
            String od = sc.nextLine();

            String sql = "INSERT INTO c3m_bloque_casas (BloVivCod, BloMetB, BloOd, BloEstReg) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, bloCod);
            stmt.setDouble(2, metB);
            stmt.setString(3, od);
            stmt.setString(4, "1");

            stmt.executeUpdate();
            System.out.println("Bloque de Casas / Edificio añadido exitosamente a la base de datos.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarBloqueCasas() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Trae la información física cruzando la jerarquía con INNER JOIN para reportar ubicaciones reales
            String sql = "SELECT b.BloVivCod, d.DirViaNom, d.DirNum, b.BloMetB, v.VivVal, b.BloOd " +
                         "FROM c3m_bloque_casas b " +
                         "INNER JOIN c3m_vivienda v ON b.BloVivCod = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE b.BloEstReg = '1' AND v.VivEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE BLOQUES DE CASAS / EDIFICIOS ACTIVOS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod Bloque: " + rs.getString("BloVivCod") + " | " +
                    "Dirección: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " | " +
                    "Área Total Bloque: " + rs.getDouble("BloMetB") + "m² | " +
                    "Autovalúo: S/ " + rs.getDouble("VivVal") + " | " +
                    "Notas: " + rs.getString("BloOd")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarBloqueCasas() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Junta los campos de la subclase con la información geográfica urbana de la municipalidad
            String sql = "SELECT b.*, d.DirViaNom, d.DirNum, d.DirUrb, v.VivUbigeo, v.VivVal, z.ZonNom " +
                         "FROM c3m_bloque_casas b " +
                         "INNER JOIN c3m_vivienda v ON b.BloVivCod = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "INNER JOIN c2m_zona z ON v.VivZon = z.ZonCod " +
                         "WHERE b.BloVivCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código del Bloque a buscar: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE CATASTRAL: BLOQUE / CONDOMINIO ---");
                System.out.println("Código Multifamiliar: " + rs.getString("BloVivCod"));
                System.out.println("Zona Territorial: " + rs.getString("ZonNom"));
                System.out.println("Ubicación Principal: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " - Urb. " + rs.getString("DirUrb"));
                System.out.println("Ubigeo Catastral: " + rs.getString("VivUbigeo"));
                System.out.println("Superficie Total del Bloque: " + rs.getDouble("BloMetB") + " m2");
                System.out.println("Valor del Autovalúo Matriz: S/ " + rs.getDouble("VivVal"));
                System.out.println("Datos de Campo (BloOd): " + rs.getString("BloOd"));
            } else {
                System.out.println("Registro de Bloque de Casas no encontrado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarBloqueCasas() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c3m_bloque_casas SET BloMetB = ?, BloOd = ? WHERE BloVivCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del bloque/edificio a modificar: ");
            String codigo = sc.nextLine();

            if (!existeId(conn, "c3m_bloque_casas", "BloVivCod", codigo)) {
                System.out.println("El bloque especificado no se encuentra registrado.");
                return;
            }

            System.out.print("Nuevos m² totales del bloque (BloMetB): ");
            double metB = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Nuevos Datos Adicionales (BloOd): ");
            String od = sc.nextLine();

            stmt.setDouble(1, metB);
            stmt.setString(2, od);
            stmt.setString(3, codigo);

            stmt.executeUpdate();
            System.out.println("Atributos del Bloque de Casas actualizados correctamente.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarBloqueCasas() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM c3m_bloque_casas WHERE BloVivCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Bloque de Casas a eliminar físicamente: ");
            String codigo = sc.nextLine();
            stmt.setString(1, codigo);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Registro eliminado físicamente de la tabla especializada.");
            } else {
                System.out.println("No se encontró el registro.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}