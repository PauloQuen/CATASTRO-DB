import java.sql.*;
import java.util.Scanner;

public class c4m_departamentoCRUD {

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
            System.out.println("\n===== CRUD DEPARTAMENTO (SUB-COMPONENTE INTERNO) =====");
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
                case 1: insertarDepartamento(); break;
                case 2: listarDepartamento(); break;
                case 3: buscarDepartamento(); break;
                case 4: actualizarDepartamento(); break;
                case 5: eliminarDepartamento(); break;
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

    public static void insertarDepartamento() {
        Connection conn = conectar();
        try {
            System.out.print("Codigo Bloque de Casas (FK): ");
            int bloCod = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial controlando que el edificio matriz exista en el catastro urbano.
            if (!existeId(conn, "c3m_bloque_casas", "BloCod", bloCod)) {
                System.out.println("Error: El Bloque de Casas indicado no existe."); return;
            }
            sc.nextLine(); // Limpieza buffer

            System.out.print("Escalera / Bloque Interno (Ejm: A, B): ");
            String esc = sc.nextLine();

            System.out.print("Número de Nivel / Piso: ");
            int niv = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Número de Puerta / Departamento (Ejm: 301): ");
            String pue = sc.nextLine();

            System.out.print("Metraje Cuadrado (Área Total): ");
            double area = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO c4m_departamento (DepBloCod, DepEsc, DepNiv, DepPue, DepArea, DepEstReg) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bloCod);
            stmt.setString(2, esc);
            stmt.setInt(3, niv);
            stmt.setString(4, pue);
            stmt.setDouble(5, area);
            stmt.setString(6, "1");

            stmt.executeUpdate();
            System.out.println("Departamento registrado con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarDepartamento() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN para resolver el código frío del bloque trayendo la dirección nominal de la calle del condominio.
            String sql = "SELECT d.DepBloCod, d.DepEsc, d.DepNiv, d.DepPue, d.DepArea, dir.DirViaNom " +
                         "FROM c4m_departamento d " +
                         "INNER JOIN c3m_bloque_casas b ON d.DepBloCod = b.BloCod " +
                         "INNER JOIN c3m_direccion dir ON b.BloDir = dir.DirCod " +
                         "WHERE d.DepEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE DEPARTAMENTOS INTERNOS ---");
            while (rs.next()) {
                System.out.println(
                    "Bloque ID: " + rs.getInt("DepBloCod") + " (" + rs.getString("DirViaNom") + ") | " +
                    "Esc: " + rs.getString("DepEsc") + " | " +
                    "Piso: " + rs.getInt("DepNiv") + " | " +
                    "Dpto/Puerta: " + rs.getString("DepPue") + " | " +
                    "Área: " + rs.getDouble("DepArea") + " m²"
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarDepartamento() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Realiza una búsqueda exacta usando la clave compuesta estructural para localizar la propiedad horizontal.
            String sql = "SELECT d.*, dir.DirViaNom FROM c4m_departamento d " +
                         "INNER JOIN c3m_bloque_casas b ON d.DepBloCod = b.BloCod " +
                         "INNER JOIN c3m_direccion dir ON b.BloDir = dir.DirCod " +
                         "WHERE d.DepBloCod=? AND d.DepEsc=? AND d.DepNiv=? AND d.DepPue=?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código Bloque Matriz: ");
            int bloCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Letra Escalera: ");
            String esc = sc.nextLine();

            System.out.print("Piso/Nivel: ");
            int niv = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Número Puerta: ");
            String pue = sc.nextLine();

            stmt.setInt(1, bloCod);
            stmt.setString(2, esc);
            stmt.setInt(3, niv);
            stmt.setString(4, pue);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- DATOS DE LA PROPIEDAD HORIZONTAL ---");
                System.out.println("Edificio Condominio : " + rs.getString("DirViaNom"));
                System.out.println("Escalera / Sector   : " + rs.getString("DepEsc"));
                System.out.println("Nivel Ubicación     : " + rs.getInt("DepNiv"));
                System.out.println("Identificador Puerta: " + rs.getString("DepPue"));
                System.out.println("Área Privativa      : " + rs.getDouble("DepArea") + " m²");
                System.out.println("Estado de Ficha     : " + rs.getString("DepEstReg"));
            } else {
                System.out.println("Departamento no encontrado en los registros.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarDepartamento() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Permite re-dimensionar el área catastrada identificando de forma única el registro por su clave compuesta.
            String sql = "UPDATE c4m_departamento SET DepArea=? WHERE DepBloCod=? AND DepEsc=? AND DepNiv=? AND DepPue=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Bloque Matriz del Dpto a modificar: ");
            int bloCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Letra Escalera: ");
            String esc = sc.nextLine();

            System.out.print("Piso/Nivel: ");
            int niv = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Número Puerta: ");
            String pue = sc.nextLine();

            System.out.print("Nuevo Metraje Cuadrado (Área Modificada): ");
            double area = sc.nextDouble();

            stmt.setDouble(1, area);
            stmt.setInt(2, bloCod);
            stmt.setString(3, esc);
            stmt.setInt(4, niv);
            stmt.setString(5, pue);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Dimensiones del departamento actualizadas.");
            else System.out.println("No se encontró la unidad inmobiliaria interna.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarDepartamento() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Ejecuta un borrado físico seguro requiriendo la clave compuesta de la propiedad inmobiliaria.
            String sql = "DELETE FROM c4m_departamento WHERE DepBloCod=? AND DepEsc=? AND DepNiv=? AND DepPue=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Bloque Matriz a remover: ");
            int bloCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Letra Escalera: ");
            String esc = sc.nextLine();

            System.out.print("Piso/Nivel: ");
            int niv = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Número Puerta: ");
            String pue = sc.nextLine();

            stmt.setInt(1, bloCod);
            stmt.setString(2, esc);
            stmt.setInt(3, niv);
            stmt.setString(4, pue);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Sub-unidad inmobiliaria purgada de la base de datos.");
            else System.out.println("No se encontró el registro indicado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}