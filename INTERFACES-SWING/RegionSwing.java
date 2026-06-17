import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class RegionSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db?useSSL=false&serverTimezone=America/Lima";
    static final String USER = "root";
    static final String PASSWORD = ""; 

    private JTextField txtCod, txtNom;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public RegionSwing() {
        setTitle("Mantenimiento de Regiones (c1m_region)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // FORMULARIO
        JPanel panelFormulario = new JPanel(new GridLayout(2, 2, 8, 8));
        txtCod = new JTextField(); txtCod.setEditable(false);
        txtNom = new JTextField();

        panelFormulario.add(new JLabel("Código Región:")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Nombre Región:")); panelFormulario.add(txtNom);

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
        modeloTabla = new DefaultTableModel(new String[]{"Código", "Nombre Región"}, 0);
        tablaDatos = new JTable(modeloTabla);
        panelPrincipal.add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        cargarDatos();

        // EVENTOS
        btnGuardar.addActionListener(e -> ejecutarSQL("INSERT INTO c1m_region (RegNom) VALUES (?)", true));
        btnModificar.addActionListener(e -> ejecutarSQL("UPDATE c1m_region SET RegNom=? WHERE RegCod=?", false));
        btnEliminar.addActionListener(e -> eliminarRegistro());

        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                txtCod.setText(modeloTabla.getValueAt(fila, 0).toString());
                txtNom.setText(modeloTabla.getValueAt(fila, 1).toString());
            }
        });
    }

    private void cargarDatos() {
        isAjustando = true;
        modeloTabla.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM c1m_region")) {
            while (rs.next()) {
                modeloTabla.addRow(new Object[]{rs.getInt("RegCod"), rs.getString("RegNom")});
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        isAjustando = false;
    }

    private void ejecutarSQL(String sql, boolean esInsert) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtNom.getText());
            if (!esInsert) ps.setInt(2, Integer.parseInt(txtCod.getText()));
            ps.executeUpdate();
            cargarDatos();
            JOptionPane.showMessageDialog(this, "Operación exitosa");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void eliminarRegistro() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM c1m_region WHERE RegCod=?")) {
            ps.setInt(1, Integer.parseInt(txtCod.getText()));
            ps.executeUpdate();
            cargarDatos();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error al eliminar"); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegionSwing().setVisible(true));
    }
}