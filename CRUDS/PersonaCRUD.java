import java.sql.*;
import java.util.Scanner;

public class PersonaCRUD {

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
            System.out.println("\n===== CRUD PERSONA =====");
            System.out.println("1. Insertar");
            System.out.println("2. Listar");
            System.out.println("3. Buscar");
            System.out.println("4. Actualizar");
            System.out.println("5. Eliminar");
            System.out.println("6. Salir");

            System.out.print("Opcion: ");
            opcion = sc.nextInt();

            switch(opcion)
            {
                case 1:
                    insertarPersona();
                    break;

                case 2:
                    listarPersona();
                    break;

                case 3:
                    buscarPersona();
                    break;

                case 4:
                    actualizarPersona();
                    break;

                case 5:
                    eliminarPersona();
                    break;
            }

        }while(opcion != 6);
    }

    // ==========================
    // INSERT
    // ==========================
    public static void insertarPersona()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "INSERT INTO H6M_PERSONA " +
            "(PerDNI,PerNom,PerApePat,PerApeMat,PerIng,PerViv,PerEstReg) " +
            "VALUES (?,?,?,?,?,?,?)";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("DNI: ");
            int dni = sc.nextInt();
            sc.nextLine();

            System.out.print("Nombre: ");
            String nom = sc.nextLine();

            System.out.print("Apellido Paterno: ");
            String apePat = sc.nextLine();

            System.out.print("Apellido Materno: ");
            String apeMat = sc.nextLine();

            System.out.print("Ingreso: ");
            double ingreso = sc.nextDouble();
            sc.nextLine();

            System.out.print("Codigo Vivienda: ");
            String viv = sc.nextLine();

            stmt.setInt(1,dni);
            stmt.setString(2,nom);
            stmt.setString(3,apePat);
            stmt.setString(4,apeMat);
            stmt.setDouble(5,ingreso);
            stmt.setString(6,viv);
            stmt.setString(7,"1");

            stmt.executeUpdate();

            System.out.println("Persona registrada.");

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
    public static void listarPersona()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "SELECT * FROM H6M_PERSONA";

            Statement stmt =
            conn.createStatement();

            ResultSet rs =
            stmt.executeQuery(sql);

            while(rs.next())
            {
                System.out.println(
                rs.getInt("PerDNI")
                + " | "
                + rs.getString("PerNom")
                + " "
                + rs.getString("PerApePat")
                + " "
                + rs.getString("PerApeMat")
                + " | "
                + rs.getDouble("PerIng"));
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
    public static void buscarPersona()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "SELECT * FROM H6M_PERSONA WHERE PerDNI=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("DNI: ");

            int dni = sc.nextInt();

            stmt.setInt(1,dni);

            ResultSet rs =
            stmt.executeQuery();

            if(rs.next())
            {
                System.out.println(
                rs.getString("PerNom"));

                System.out.println(
                rs.getString("PerApePat"));

                System.out.println(
                rs.getString("PerApeMat"));
            }
            else
            {
                System.out.println("No encontrado");
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
    public static void actualizarPersona()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "UPDATE H6M_PERSONA " +
            "SET PerNom=?, PerIng=? " +
            "WHERE PerDNI=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("DNI: ");
            int dni = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre: ");
            String nombre = sc.nextLine();

            System.out.print("Nuevo Ingreso: ");
            double ingreso = sc.nextDouble();

            stmt.setString(1,nombre);
            stmt.setDouble(2,ingreso);
            stmt.setInt(3,dni);

            stmt.executeUpdate();

            System.out.println("Actualizado.");

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
    public static void eliminarPersona()
    {
        Connection conn = conectar();

        try
        {
            String sql =
            "DELETE FROM H6M_PERSONA " +
            "WHERE PerDNI=?";

            PreparedStatement stmt =
            conn.prepareStatement(sql);

            System.out.print("DNI: ");

            int dni = sc.nextInt();

            stmt.setInt(1,dni);

            stmt.executeUpdate();

            System.out.println("Eliminado.");

            stmt.close();
            conn.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}