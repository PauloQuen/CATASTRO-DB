import java.sql.*;
import java.util.Scanner;

public class p9m_escala_tributoCRUD {

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
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conn;
    }

    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD ESCALA TRIBUTARIA REFERENCIAL =====");
            System.out.println("1. Insertar Escala Tributaria");
            System.out.println("2. Listar Escalas Vigentes");
            System.out.println("3. Buscar por Clave Compuesta (Código y Vigencia)");
            System.out.println("4. Actualizar Valores de Escala");
            System.out.println("5. Eliminar Escala");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarEscala(); break;
                case 2: listarEscalas(); break;
                case 3: buscarEscala(); break;
                case 4: actualizarEscala(); break;
                case 5: eliminarEscala(); break;
            }
        } while (opcion != 6);
    }

    // ====================================================================
    // METODO AUXILIAR: Valida la existencia de la clave primaria compuesta
    // ====================================================================
    private static boolean existeEscala(Connection conn, int escCod, int escVig) throws SQLException {
        String sql = "SELECT COUNT(*) FROM p9m_escala_tributo WHERE EscCod = ? AND EscVig = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, escCod);
            stmt.setInt(2, escVig);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static void insertarEscala() {
        Connection conn = conectar();
        try {
            System.out.print("Código de la Escala (EscCod - Int): ");
            int escCod = sc.nextInt();

            System.out.print("Año de Vigencia (EscVig - Int Ejm: 2026): ");
            int escVig = sc.nextInt();

            // VALIDACIÓN: Evita duplicar la clave compuesta (EscCod, EscVig)
            if (existeEscala(conn, escCod, escVig)) {
                System.out.println("ERROR: Ya existe una escala con ese código y año de vigencia. Operación cancelada.");
                conn.close();
                return;
            }
            sc.nextLine(); // Limpiar buffer

            System.out.print("Descripción del Tributo (Arbitrios/Impuestos): ");
            String desc = sc.nextLine();

            System.out.print("Monto Base Calculado S/: ");
            double monCal = sc.nextDouble();

            System.out.print("Monto Tope Máximo S/: ");
            double monTop = sc.nextDouble();
            sc.nextLine(); // Limpiar buffer

            String sql = "INSERT INTO p9m_escala_tributo (EscCod, EscVig, EscDes, EscMonCal, EscMonTop, EscEstReg) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, escCod);
            stmt.setInt(2, escVig);
            stmt.setString(3, desc);
            stmt.setDouble(4, monCal);
            stmt.setDouble(5, monTop);
            stmt.setString(6, "1");

            stmt.executeUpdate();
            System.out.println("Escala tributaria configurada con éxito.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarEscalas() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM p9m_escala_tributo WHERE EscEstReg = '1' ORDER BY EscVig DESC, EscCod ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- MAESTRO DE ESCALAS TRIBUTARIAS ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod: " + rs.getInt("EscCod") + " | " +
                    "Vigencia: " + rs.getInt("EscVig") + " | " +
                    "Desc: " + rs.getString("EscDes") + " | " +
                    "Monto Base: S/ " + rs.getDouble("EscMonCal") + " | " +
                    "Tope Máx: S/ " + rs.getDouble("EscMonTop")
                );
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarEscala() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM p9m_escala_tributo WHERE EscCod = ? AND EscVig = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Escala a buscar: ");
            int escCod = sc.nextInt();
            System.out.print("Ingrese el Año de Vigencia de la escala: ");
            int escVig = sc.nextInt();

            stmt.setInt(1, escCod);
            stmt.setInt(2, escVig);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- CONFIGURACIÓN DE PARÁMETROS ENCONTRADA ---");
                System.out.println("Código Escala: " + rs.getInt("EscCod"));
                System.out.println("Año Vigencia : " + rs.getInt("EscVig"));
                System.out.println("Descripción  : " + rs.getString("EscDes"));
                System.out.println("Monto Base   : S/ " + rs.getDouble("EscMonCal"));
                System.out.println("Monto Tope   : S/ " + rs.getDouble("EscMonTop"));
                System.out.println("Reg. Estado  : " + rs.getString("EscEstReg"));
            } else {
                System.out.println("Escala tributaria no encontrada.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarEscala() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE p9m_escala_tributo SET EscDes = ?, EscMonCal = ?, EscMonTop = ? WHERE EscCod = ? AND EscVig = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de la Escala a modificar: ");
            int escCod = sc.nextInt();
            System.out.print("Ingrese el Año de Vigencia de la escala a modificar: ");
            int escVig = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nueva Descripción: ");
            String desc = sc.nextLine();

            System.out.print("Nuevo Monto Base S/: ");
            double monCal = sc.nextDouble();

            System.out.print("Nuevo Monto Tope S/: ");
            double monTop = sc.nextDouble();

            stmt.setString(1, desc);
            stmt.setDouble(2, monCal);
            stmt.setDouble(3, monTop);
            stmt.setInt(4, escCod);
            stmt.setInt(5, escVig);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Parámetros de la escala actualizados correctamente.");
            } else {
                System.out.println("No se encontró ningún registro con esa combinación de claves.");
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarEscala() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM p9m_escala_tributo WHERE EscCod = ? AND EscVig = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Escala a eliminar: ");
            int escCod = sc.nextInt();
            System.out.print("Ingrese el Año de Vigencia a eliminar: ");
            int escVig = sc.nextInt();

            stmt.setInt(1, escCod);
            stmt.setInt(2, escVig);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Escala eliminada físicamente del sistema.");
            } else {
                System.out.println("No se encontró el registro.");
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            // EXPLICACIÓN: Captura restricción si un propietario (h8m_propietario) ya está usando esta escala para liquidar sus cuentas.
            System.out.println("ERROR: No se puede eliminar. Existen contribuyentes asociados a esta tasa.");
        }
    }
}
