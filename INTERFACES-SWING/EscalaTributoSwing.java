import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.math.BigDecimal;

public class EscalaTributoSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db?useSSL=false&serverTimezone=America/Lima";
    static final String USER = "root";
    static final String PASSWORD = "";
    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private JTextField txtCod, txtVig, txtNom, txtDesc, txtIngMin, txtIngMax, txtPorc, txtMonFij;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public EscalaTributoSwing() {
        setTitle("Mantenimiento Escala Tributaria (p9m_escala_tributo)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 650);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // FORMULARIO DE 8 CAMPOS
        JPanel panelFormulario = new JPanel(new GridLayout(8, 2, 5, 5));
        txtCod = new JTextField(); txtVig = new JTextField();
        txtNom = new JTextField(); txtDesc = new JTextField();
        txtIngMin = new JTextField(); txtIngMax = new JTextField();
        txtPorc = new JTextField(); txtMonFij = new JTextField();

        panelFormulario.add(new JLabel("Código (EscCod):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Año (EscVig):")); panelFormulario.add(txtVig);
        panelFormulario.add(new JLabel("Nombre (EscNom):")); panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Descripción:")); panelFormulario.add(txtDesc);
        panelFormulario.add(new JLabel("Ingreso Mín:")); panelFormulario.add(txtIngMin);
        panelFormulario.add(new JLabel("Ingreso Máx:")); panelFormulario.add(txtIngMax);
        panelFormulario.add(new JLabel("Porcentaje %:")); panelFormulario.add(txtPorc);
        panelFormulario.add(new JLabel("Monto Fijo:")); panelFormulario.add(txtMonFij);

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
        modeloTabla = new DefaultTableModel(new String[]{"Cod", "Año", "Nom", "Desc", "IngMin", "IngMax", "%", "Fijo"}, 0);
        tablaDatos = new JTable(modeloTabla);
        panelPrincipal.add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        cargarDatos();

        // ACCIONES
        btnGuardar.addActionListener(e -> ejecutarSQL("INSERT INTO p9m_escala_tributo (EscCod, EscVig, EscNom, EscDesc, EscIngMin, EscIngMax, EscPorTrib, EscMonFij, EscEstReg) VALUES (?,?,?,?,?,?,?,?,?)", true));
        btnModificar.addActionListener(e -> ejecutarSQL("UPDATE p9m_escala_tributo SET EscNom=?, EscDesc=?, EscIngMin=?, EscIngMax=?, EscPorTrib=?, EscMonFij=? WHERE EscCod=? AND EscVig=?", false));
        btnEliminar.addActionListener(e -> eliminarRegistro());

        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int f = tablaDatos.getSelectedRow();
            if (f != -1) {
                txtCod.setText(modeloTabla.getValueAt(f, 0).toString());
                txtVig.setText(modeloTabla.getValueAt(f, 1).toString());
                txtNom.setText(modeloTabla.getValueAt(f, 2).toString());
                txtDesc.setText(modeloTabla.getValueAt(f, 3).toString());
                txtIngMin.setText(modeloTabla.getValueAt(f, 4).toString());
                txtIngMax.setText(modeloTabla.getValueAt(f, 5).toString());
                txtPorc.setText(modeloTabla.getValueAt(f, 6) != null ? modeloTabla.getValueAt(f, 6).toString() : "");
                txtMonFij.setText(modeloTabla.getValueAt(f, 7) != null ? modeloTabla.getValueAt(f, 7).toString() : "");
            }
        });
    }

    private void cargarDatos() {
        isAjustando = true;
        modeloTabla.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM p9m_escala_tributo WHERE EscEstReg = '1'")) {
            while (rs.next()) {
                modeloTabla.addRow(new Object[]{rs.getString("EscCod"), rs.getInt("EscVig"), rs.getString("EscNom"), rs.getString("EscDesc"), 
                                   rs.getBigDecimal("EscIngMin"), rs.getBigDecimal("EscIngMax"), rs.getBigDecimal("EscPorTrib"), rs.getBigDecimal("EscMonFij")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        isAjustando = false;
    }

    private void ejecutarSQL(String sql, boolean esInsert) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (esInsert) {
                ps.setString(1, txtCod.getText()); ps.setInt(2, Integer.parseInt(txtVig.getText()));
                ps.setString(3, txtNom.getText()); ps.setString(4, txtDesc.getText());
                ps.setBigDecimal(5, new BigDecimal(txtIngMin.getText()));
                ps.setBigDecimal(6, new BigDecimal(txtIngMax.getText()));
                
                if(txtPorc.getText().trim().isEmpty()) ps.setNull(7, Types.DECIMAL);
                else ps.setBigDecimal(7, new BigDecimal(txtPorc.getText()));
                
                if(txtMonFij.getText().trim().isEmpty()) ps.setNull(8, Types.DECIMAL);
                else ps.setBigDecimal(8, new BigDecimal(txtMonFij.getText()));
                
                ps.setString(9, "1");
            } else {
                ps.setString(1, txtNom.getText()); ps.setString(2, txtDesc.getText());
                ps.setBigDecimal(3, new BigDecimal(txtIngMin.getText()));
                ps.setBigDecimal(4, new BigDecimal(txtIngMax.getText()));
                
                if(txtPorc.getText().trim().isEmpty()) ps.setNull(5, Types.DECIMAL);
                else ps.setBigDecimal(5, new BigDecimal(txtPorc.getText()));
                
                if(txtMonFij.getText().trim().isEmpty()) ps.setNull(6, Types.DECIMAL);
                else ps.setBigDecimal(6, new BigDecimal(txtMonFij.getText()));
                
                ps.setString(7, txtCod.getText()); ps.setInt(8, Integer.parseInt(txtVig.getText()));
            }
            ps.executeUpdate();
            cargarDatos();
            JOptionPane.showMessageDialog(this, "Operación exitosa");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void eliminarRegistro() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM p9m_escala_tributo WHERE EscCod=? AND EscVig=?")) {
            ps.setString(1, txtCod.getText()); ps.setInt(2, Integer.parseInt(txtVig.getText()));
            ps.executeUpdate();
            cargarDatos();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EscalaTributoSwing().setVisible(true));
    }
}