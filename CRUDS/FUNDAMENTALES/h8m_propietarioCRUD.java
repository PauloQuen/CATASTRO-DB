import java.sql.*;
import java.util.Scanner;

public class h8m_propietarioCRUD {

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
            System.out.println("\n===== CRUD MAESTRO PROPIETARIOS (UNIFICADO) =====");
            System.out.println("1. Registrar Nuevo Propietario");
            System.out.println("2. Listar Padrón de Propietarios");
            System.out.println("3. Buscar Propietario por Código");
            System.out.println("4. Actualizar Escala Tributaria Asignada");
            System.out.println("5. Eliminar Registro (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            switch (opcion) {
                case 1: insertarPropietario(); break;
                case 2: listarPropietarios(); break;
                case 3: buscarPropietario(); break;
                case 4: actualizarEscalaPropietario(); break;
                case 5: eliminarPropietario(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para llaves simples (Persona o Vivienda)
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

    // EXPLICACIÓN: Validador específico para la FK compuesta referencial hacia p9m_escala_tributo
    private static boolean existeEscala(Connection conn, String escCod, int escVig) throws SQLException {
        String sql = "SELECT COUNT(*) FROM p9m_escala_tributo WHERE EscCod = ? AND EscVig = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escCod);
            stmt.setInt(2, escVig);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarPropietario() {
        Connection conn = conectar();
        try {
            System.out.print("DNI de la Persona (ProPer): ");
            int proPer = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existeId(conn, "h6m_persona", "PerDNI", proPer)) {
                System.out.println("Error: El DNI de la persona no existe en el maestro H6M_PERSONA.");
                return;
            }

            System.out.print("Código de Vivienda Principal (ProViv) [Dejar vacío si no tiene]: ");
            String proViv = sc.nextLine().trim();
            if (!proViv.isEmpty() && !existeId(conn, "c3m_vivienda", "VivCod", proViv)) {
                System.out.println("Error: El código de vivienda ingresado no existe.");
                return;
            }

            System.out.print("Código de Escala Tributaria Referencial (Ejm: E01) [Dejar vacío si aplica NULL]: ");
            String proEscCod = sc.nextLine().trim();
            
            Integer proEscVig = null;
            String proFecCla = null;
            
            if (!proEscCod.isEmpty()) {
                System.out.print("Año de Vigencia de la Escala (Ejm: 2026): ");
                proEscVig = sc.nextInt();
                sc.nextLine(); // Limpieza buffer

                if (!existeEscala(conn, proEscCod, proEscVig)) {
                    System.out.println("Error: La escala tributaria " + proEscCod + " para el año " + proEscVig + " no existe.");
                    return;
                }

                System.out.print("Fecha de Clasificación de la Escala (YYYY-MM-DD): ");
                proFecCla = sc.nextLine();
            }

            String sql = "INSERT INTO h8m_propietario (ProPer, ProViv, ProEscCod, ProEscVig, ProFecCla, ProEstReg) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, proPer);
            
            if (proViv.isEmpty()) stmt.setNull(2, Types.VARCHAR);
            else stmt.setString(2, proViv);

            if (proEscCod.isEmpty()) {
                stmt.setNull(3, Types.VARCHAR);
                stmt.setNull(4, Types.INTEGER);
                stmt.setNull(5, Types.DATE);
            } else {
                stmt.setString(3, proEscCod);
                stmt.setInt(4, proEscVig);
                stmt.setDate(5, Date.valueOf(proFecCla));
            }
            stmt.setString(6, "1");

            stmt.executeUpdate();
            System.out.println("Contribuyente registrado exitosamente como propietario.");
            stmt.close(); conn.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Formato de fecha de clasificación incorrecto. Use YYYY-MM-DD.");
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarPropietarios() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Se cruza relacionalmente con H6M_PERSONA para armar el padrón nominal legible
            String sql = "SELECT pr.ProCod, pr.ProPer, CONCAT(p.PerNom, ' ', p.PerApePat) AS Ciudadano, " +
                         "pr.ProViv, pr.ProEscCod, pr.ProEscVig " +
                         "FROM h8m_propietario pr " +
                         "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                         "WHERE pr.ProEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- PADRÓN GENERAL DE PROPIETARIOS Y CONTRIBUYENTES ---");
            while (rs.next()) {
                String viv = rs.getString("ProViv");
                String esc = rs.getString("ProEscCod");
                System.out.println(
                    "Código Prop: " + rs.getInt("ProCod") + " | " +
                    "Contribuyente: " + rs.getString("Ciudadano") + " (DNI: " + rs.getInt("ProPer") + ") | " +
                    "Predio Principal: " + (viv != null ? viv : "SIN ASIGNAR") + " | " +
                    "Escala Ref: " + (esc != null ? (esc + " (" + rs.getInt("ProEscVig") + ")") : "NINGUNA")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPropietario() {
        Connection conn = conectar();
        try {
            String sql = "SELECT pr.*, CONCAT(p.PerNom, ' ', p.PerApePat, ' ', p.PerApeMat) AS NombreCompleto, " +
                         "e.EscNom, e.EscPorTrib, e.EscMonFij " +
                         "FROM h8m_propietario pr " +
                         "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                         "LEFT JOIN p9m_escala_tributo e ON pr.ProEscCod = e.EscCod AND pr.ProEscVig = e.EscVig " +
                         "WHERE pr.ProCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese Código de Propietario a consultar: ");
            int proCod = sc.nextInt();
            stmt.setInt(1, proCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE DE PROPIETARIO ---");
                System.out.println("Código Propietario    : " + rs.getInt("ProCod"));
                System.out.println("Titular Completo      : " + rs.getString("NombreCompleto"));
                System.out.println("DNI Identificador     : " + rs.getInt("ProPer"));
                System.out.println("Vivienda Declarada    : " + (rs.getString("ProViv") != null ? rs.getString("ProViv") : "No vinculada"));
                
                String escCod = rs.getString("ProEscCod");
                if (escCod != null) {
                    System.out.println("Escala Configurada    : " + escCod + " - " + rs.getString("EscNom") + " (" + rs.getInt("ProEscVig") + ")");
                    System.out.println("Fecha Clasificación   : " + rs.getDate("ProFecCla"));
                    System.out.println("Factor / Monto Fijo   : % " + rs.getDouble("EscPorTrib") + " / S/ " + rs.getDouble("EscMonFij"));
                } else {
                    System.out.println("Escala Configurada    : NINGUNA (Exento / Sin clasificar)");
                }
            } else {
                System.out.println("No se encontró ningún propietario con el código especificado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarEscalaPropietario() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE h8m_propietario SET ProEscCod = ?, ProEscVig = ?, ProFecCla = ? WHERE ProCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Propietario a reclasificar: ");
            int proCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existeId(conn, "h8m_propietario", "ProCod", proCod)) {
                System.out.println("No existe el registro de propietario indicado.");
                return;
            }

            System.out.print("Nuevo Código de Escala Referencial (Ejm: E02): ");
            String proEscCod = sc.nextLine().trim();

            System.out.print("Año Vigencia de la Nueva Escala: ");
            int proEscVig = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existeEscala(conn, proEscCod, proEscVig)) {
                System.out.println("La escala referencial especificada no existe en la configuración tributaria.");
                return;
            }

            System.out.print("Nueva Fecha de Reclasificación (YYYY-MM-DD): ");
            String proFecCla = sc.nextLine();

            stmt.setString(1, proEscCod);
            stmt.setInt(2, proEscVig);
            stmt.setDate(3, Date.valueOf(proFecCla));
            stmt.setInt(4, proCod);

            stmt.executeUpdate();
            System.out.println("Escala tributaria de referencia actualizada correctamente para el propietario.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarPropietario() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM h8m_propietario WHERE ProCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de Propietario a eliminar de los registros físicos: ");
            int proCod = sc.nextInt();
            stmt.setInt(1, proCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Registro de propietario purgado físicamente del sistema catastral.");
            } else {
                System.out.println("No se localizó el código de propietario.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: Compruebe que el propietario no tenga tributos vinculados (Restricción de Integridad).");
        }
    }
}
