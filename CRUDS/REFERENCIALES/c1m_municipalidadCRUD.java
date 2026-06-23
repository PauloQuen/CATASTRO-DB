import java.sql.*;
import java.util.Scanner;

public class c1m_municipalidadCRUD {

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
            System.out.println("\n===== CRUD MUNICIPALIDAD =====");
            System.out.println("1. Insertar Municipalidad");
            System.out.println("2. Listar Municipalidades");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Municipalidad");
            System.out.println("5. Eliminar Municipalidad");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarMunicipalidad(); break;
                case 2: listarMunicipalidades(); break;
                case 3: buscarMunicipalidad(); break;
                case 4: actualizarMunicipalidad(); break;
                case 5: eliminarMunicipalidad(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarMunicipalidad() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c1m_municipalidad " +
                         "(MunCod, MunDis, MunNom, MunAlcApePat, MunAlcApeMat, MunAlcNom, MunPreAnu, MunEstReg) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Municipalidad (Int): ");
            int munCod = sc.nextInt();

            System.out.print("Código Distrito FK (Int): ");
            int munDis = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nombre de la Municipalidad: ");
            String munNom = sc.nextLine();

            System.out.print("Apellido Paterno Alcalde: ");
            String alcApePat = sc.nextLine();

            System.out.print("Apellido Materno Alcalde: ");
            String alcApeMat = sc.nextLine();

            System.out.print("Nombres Alcalde: ");
            String alcNom = sc.nextLine();

            System.out.print("Presupuesto Anual (Decimal): ");
            double preAnu = sc.nextDouble();
            sc.nextLine(); // Limpiar buffer

            stmt.setInt(1, munCod);
            stmt.setInt(2, munDis);
            stmt.setString(3, munNom);
            stmt.setString(4, alcApePat);
            stmt.setString(5, alcApeMat);
            stmt.setString(6, alcNom);
            stmt.setDouble(7, preAnu);
            stmt.setString(8, "1");

            stmt.executeUpdate();
            System.out.println("Municipalidad registrada correctamente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarMunicipalidades() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c1m_municipalidad WHERE MunEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE MUNICIPALIDADES ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod: " + rs.getInt("MunCod") + " | " +
                    "Nombre: " + rs.getString("MunNom") + " | " +
                    "Alcalde: " + rs.getString("MunAlcNom") + " " + rs.getString("MunAlcApePat") + " | " +
                    "Presupuesto: S/ " + rs.getDouble("MunPreAnu")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarMunicipalidad() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c1m_municipalidad WHERE MunCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Municipalidad a buscar: ");
            int munCod = sc.nextInt();

            stmt.setInt(1, munCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS DE LA MUNICIPALIDAD ---");
                System.out.println("Código: " + rs.getInt("MunCod"));
                System.out.println("Código Distrito (FK): " + rs.getInt("MunDis"));
                System.out.println("Nombre: " + rs.getString("MunNom"));
                System.out.println("Alcalde: " + rs.getString("MunAlcNom") + " " + rs.getString("MunAlcApePat") + " " + rs.getString("MunAlcApeMat"));
                System.out.println("Presupuesto Anual: S/ " + rs.getDouble("MunPreAnu"));
                System.out.println("Estado Registro: " + rs.getString("MunEstReg"));
            } else {
                System.out.println("Municipalidad no encontrada.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarMunicipalidad() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_municipalidad " +
                         "SET MunNom = ?, MunAlcNom = ?, MunPreAnu = ? " +
                         "WHERE MunCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Municipalidad a modificar: ");
            int munCod = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nuevo Nombre de Municipalidad: ");
            String nom = sc.nextLine();

            System.out.print("Nuevo Nombre de Alcalde: ");
            String alc = sc.nextLine();

            System.out.print("Nuevo Presupuesto Anual: ");
            double pre = sc.nextDouble();

            stmt.setString(1, nom);
            stmt.setString(2, alc);
            stmt.setDouble(3, pre);
            stmt.setInt(4, munCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Municipalidad actualizada correctamente.");
            } else {
                System.out.println("No se encontró el registro.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarMunicipalidad() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM c1m_municipalidad WHERE MunCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Municipalidad a eliminar: ");
            int munCod = sc.nextInt();

            stmt.setInt(1, munCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Municipalidad eliminada de la base de datos.");
            } else {
                System.out.println("No se encontró el registro.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}