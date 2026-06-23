import java.sql.*;
import java.util.Scanner;

public class c5m_valor_catastralCRUD {

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
            System.out.println("\n===== CRUD VALOR CATASTRAL (HISTÓRICO ECONÓMICO) =====");
            System.out.println("1. Insertar Valor Anual");
            System.out.println("2. Listar Historial de Valores (Con INNER JOINs)");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Monto de Valoración");
            System.out.println("5. Eliminar Registro");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarValorCatastral(); break;
                case 2: listarValorCatastral(); break;
                case 3: buscarValorCatastral(); break;
                case 4: actualizarValorCatastral(); break;
                case 5: eliminarValorCatastral(); break;
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

    public static void insertarValorCatastral() {
        Connection conn = conectar();
        try {
            System.out.print("Codigo Valor Catastral (ValCod - Int): ");
            int valCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            System.out.print("Codigo Vivienda FK (FKValViv - String): ");
            String fkValViv = sc.nextLine();
            // EXPLICACIÓN: Valida integridad referencial confirmando que la vivienda a tasar ya esté empadronada en el sistema.
            if (!existeId(conn, "c3m_vivienda", "VivCod", fkValViv)) {
                System.out.println("Error: La vivienda indicada no existe."); return;
            }

            System.out.print("Año de la Valoración (ValAno - Int Ejm: 2026): ");
            int valAno = sc.nextInt();

            System.out.print("Monto del Valor Catastral S/: ");
            double valMon = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO c5m_valor_catastral (ValCod, FKValViv, ValAno, ValMon, ValEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, valCod);
            stmt.setString(2, fkValViv);
            stmt.setInt(3, valAno);
            stmt.setDouble(4, valMon);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("Valoración catastral registrada con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void listarValorCatastral() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN múltiple para resolver la calle y número de la vivienda vinculada al historial económico.
            String sql = "SELECT vc.ValCod, vc.FKValViv, vc.ValAno, vc.ValMon, d.DirViaNom, d.DirNum " +
                         "FROM c5m_valor_catastral vc " +
                         "INNER JOIN c3m_vivienda v ON vc.FKValViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE vc.ValEstReg = '1' ORDER BY vc.ValAno DESC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- HISTORIAL DE VALORACIONES ECONÓMICAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod Valor: " + rs.getInt("ValCod") + " | " +
                    "Viv ID: " + rs.getString("FKValViv") + " (" + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + ") | " +
                    "Año: " + rs.getInt("ValAno") + " | " +
                    "Valor Autovalúo: S/ " + rs.getDouble("ValMon")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarValorCatastral() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Recupera la tasación individual cruzando datos con la ubicación física de la propiedad.
            String sql = "SELECT vc.*, d.DirViaNom, d.DirNum FROM c5m_valor_catastral vc " +
                         "INNER JOIN c3m_vivienda v ON vc.FKValViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE vc.ValCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código de Valor Catastral a buscar: ");
            int valCod = sc.nextInt();
            stmt.setInt(1, valCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- DETALLE DE VALORACIÓN ---");
                System.out.println("Código Registro  : " + rs.getInt("ValCod"));
                System.out.println("Código Predio    : " + rs.getString("FKValViv"));
                System.out.println("Ubicación Física : " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum"));
                System.out.println("Año Fiscal Tasado: " + rs.getInt("ValAno"));
                System.out.println("Monto Autovalúo  : S/ " + rs.getDouble("ValMon"));
                System.out.println("Estado Ficha     : " + rs.getString("ValEstReg"));
            } else {
                System.out.println("Registro de valoración no encontrado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarValorCatastral() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Permite rectificar el monto monetario tasado de un registro identificándolo por su ID único.
            String sql = "UPDATE c5m_valor_catastral SET ValMon=? WHERE ValCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Valor Catastral a modificar: ");
            int valCod = sc.nextInt();

            System.out.print("Nuevo Monto de Autovalúo S/: ");
            double nuevoMonto = sc.nextDouble();

            stmt.setDouble(1, nuevoMonto);
            stmt.setInt(2, valCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Monto de autovalúo rectificado correctamente.");
            else System.out.println("No se encontró el registro de tasación.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarValorCatastral() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Ejecuta un borrado físico directo del registro de valor catastral seleccionado.
            String sql = "DELETE FROM c5m_valor_catastral WHERE ValCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Valor Catastral a eliminar físicamente: ");
            int valCod = sc.nextInt();
            stmt.setInt(1, valCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Registro económico purgado del sistema.");
            else System.out.println("No se localizó el registro indicado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
