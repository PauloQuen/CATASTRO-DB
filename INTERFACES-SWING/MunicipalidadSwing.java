import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class MunicipalidadSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db?useSSL=false&serverTimezone=America/Lima";
    static final String USER = "root";
    static final String PASSWORD = "";
    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private JTextField txtCod, txtDis, txtNom, txtAlc;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public MunicipalidadSwing() {
        setTitle("Módulo de Municipalidades (c1m_municipalidad)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // FORMULARIO
        JPanel panelFormulario = new JPanel(new GridLayout(4, 2, 8, 8));
        txtCod = new JTextField(); txtCod.setEditable(false); // Es Auto_Increment
        txtDis = new JTextField(); 
        txtNom = new JTextField();
        txtAlc = new JTextField();

        panelFormulario.add(new JLabel("Código (Auto):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("ID Distrito (FK):")); panelFormulario.add(txtDis);
        panelFormulario.add(new JLabel("Nombre Municipalidad:")); panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Nombre Alcalde:")); panelFormulario.add(txtAlc);

        // BOTONES
        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnGuardar = new JButton("Registrar");
        JButton btnModificar = new JButton("Modificar");
        JButton btnEliminar = new JButton("Eliminar");
        panelBotones.add(btnGuardar); panelBotones.add(btnModificar); panelBotones.add(btnEliminar);

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // TABLA
        modeloTabla = new DefaultTableModel(new String[]{"ID", "Distrito", "Nombre Mun", "Alcalde"}, 0);
        tablaDatos = new JTable(modeloTabla);
        panelPrincipal.add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        cargarDatos();

        // EVENTOS
        btnGuardar.addActionListener(e -> ejecutarSQL("INSERT INTO c1m_municipalidad (MunDis, MunNom, MunAlcNom) VALUES (?, ?, ?)", true));
        btnModificar.addActionListener(e -> ejecutarSQL("UPDATE c1m_municipalidad SET MunDis=?, MunNom=?, MunAlcNom=? WHERE MunCod=?", false));
        btnEliminar.addActionListener(e -> eliminarRegistro());

        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                txtCod.setText(modeloTabla.getValueAt(fila, 0).toString());
                txtDis.setText(modeloTabla.getValueAt(fila, 1).toString());
                txtNom.setText(modeloTabla.getValueAt(fila, 2).toString());
                txtAlc.setText(modeloTabla.getValueAt(fila, 3).toString());
            }
        });
    }

    private void cargarDatos() {
        isAjustando = true;
        modeloTabla.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM c1m_municipalidad")) {
            while (rs.next()) {
                modeloTabla.addRow(new Object[]{rs.getInt("MunCod"), rs.getInt("MunDis"), rs.getString("MunNom"), rs.getString("MunAlcNom")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        isAjustando = false;
    }

    private void ejecutarSQL(String sql, boolean esInsert) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(txtDis.getText()));
            ps.setString(2, txtNom.getText());
            ps.setString(3, txtAlc.getText());
            if (!esInsert) ps.setInt(4, Integer.parseInt(txtCod.getText()));
            ps.executeUpdate();
            cargarDatos();
            JOptionPane.showMessageDialog(this, "Operación exitosa");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void eliminarRegistro() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM c1m_municipalidad WHERE MunCod=?")) {
            ps.setInt(1, Integer.parseInt(txtCod.getText()));
            ps.executeUpdate();
            cargarDatos();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error al eliminar"); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MunicipalidadSwing().setVisible(true));
    }
}