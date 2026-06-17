import java.sql.*;
import java.util.Scanner;

public class ViviendaCRUD {

    static final String URL =
            "jdbc:mysql://localhost:3306/catastro_db";

    static final String USER = "root";
    static final String PASSWORD = "";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args)
    {
        menuPrincipal();
    }

    // ==========================
    // CONEXION
    // ==========================
    public static Connection conectar()
    {
        Connection conn = null;

        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD);

        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }

        return conn;
    }

    // ==========================
    // MENU
    // ==========================
    public static void menuPrincipal()
    {
        int opcion;

        do
        {
            System.out.println("\n===== CRUD VIVIENDA =====");
            System.out.println("1. Insertar");
            System.out.println("2. Listar");
            System.out.println("3. Buscar");
            System.out.println("4. Actualizar");
            System.out.println("5. Eliminar");
            System.out.println("6. Salir");

            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine();

            switch(opcion)
            {
                case 1:
                    insertarVivienda();
                    break;

                case 2:
                    listarVivienda();
                    break;

                case 3:
                    buscarVivienda();
                    break;

                case 4:
                    actualizarVivienda();
                    break;

                case 5:
                    eliminarVivienda();
                    break;
            }

        } while(opcion != 6);
    }

    // ==========================
    // INSERT
    // ==========================
    public static void insertarVivienda()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "INSERT INTO C3M_VIVIENDA " +
            "(VivCod,VivZon,VivDir,VivUbigeo," +
            "VivTipPr,VivUsoPr,VivVal,VivEstReg) " +
            "VALUES (?,?,?,?,?,?,?,?)";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda: ");
            String cod = sc.nextLine();

            System.out.print("Zona: ");
            int zona = sc.nextInt();

            System.out.print("Direccion: ");
            int dir = sc.nextInt();
            sc.nextLine();

            System.out.print("Ubigeo: ");
            String ubigeo = sc.nextLine();

            System.out.print("Tipo Predio: ");
            String tipo = sc.nextLine();

            System.out.print("Uso Predio: ");
            String uso = sc.nextLine();

            System.out.print("Valor Catastral: ");
            double valor = sc.nextDouble();

            stmt.setString(1,cod);
            stmt.setInt(2,zona);
            stmt.setInt(3,dir);
            stmt.setString(4,ubigeo);
            stmt.setString(5,tipo);
            stmt.setString(6,uso);
            stmt.setDouble(7,valor);
            stmt.setString(8,"1");

            stmt.executeUpdate();

            System.out.println("Vivienda registrada.");

            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    // ==========================
    // LIST
    // ==========================
    public static void listarVivienda()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "SELECT * FROM C3M_VIVIENDA";

            Statement stmt =
            conn.createStatement();

            ResultSet rs =
            stmt.executeQuery(sql);

            while(rs.next())
            {
                System.out.println(
                rs.getString("VivCod")
                + " | "
                + rs.getString("VivUbigeo")
                + " | "
                + rs.getString("VivTipPr")
                + " | "
                + rs.getString("VivUsoPr")
                + " | "
                + rs.getDouble("VivVal"));
            }

            rs.close();
            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    // ==========================
    // SEARCH
    // ==========================
    public static void buscarVivienda()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "SELECT * FROM C3M_VIVIENDA " +
            "WHERE VivCod=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda: ");
            String codigo = sc.nextLine();

            stmt.setString(1,codigo);

            ResultSet rs =
            stmt.executeQuery();

            if(rs.next())
            {
                System.out.println("Codigo: "
                        + rs.getString("VivCod"));

                System.out.println("Zona: "
                        + rs.getInt("VivZon"));

                System.out.println("Direccion: "
                        + rs.getInt("VivDir"));

                System.out.println("Ubigeo: "
                        + rs.getString("VivUbigeo"));

                System.out.println("Tipo: "
                        + rs.getString("VivTipPr"));

                System.out.println("Uso: "
                        + rs.getString("VivUsoPr"));

                System.out.println("Valor: "
                        + rs.getDouble("VivVal"));
            }
            else
            {
                System.out.println("No encontrada.");
            }

            rs.close();
            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    // ==========================
    // UPDATE
    // ==========================
    public static void actualizarVivienda()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "UPDATE C3M_VIVIENDA " +
            "SET VivVal=? " +
            "WHERE VivCod=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda: ");
            String codigo = sc.nextLine();

            System.out.print("Nuevo Valor: ");
            double valor = sc.nextDouble();

            stmt.setDouble(1,valor);
            stmt.setString(2,codigo);

            stmt.executeUpdate();

            System.out.println("Actualizada.");

            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    // ==========================
    // DELETE
    // ==========================
    public static void eliminarVivienda()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "DELETE FROM C3M_VIVIENDA " +
            "WHERE VivCod=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("Codigo Vivienda: ");
            String codigo = sc.nextLine();

            stmt.setString(1,codigo);

            stmt.executeUpdate();

            System.out.println("Eliminada.");

            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}