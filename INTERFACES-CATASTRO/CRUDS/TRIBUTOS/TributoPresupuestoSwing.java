import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TributoPresupuestoSwing extends JFrame {

    // Configuración de la conexión a la Base de Datos
    static final String URL = "jdbc:mysql://localhost:3306/catastro_db"
                            + "?useSSL=false&serverTimezone=America/Lima"
                            + "&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Componentes del Formulario Gráfico
    private JTextField txtTpPre, txtTpTri, txtTpMonAporte;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public TributoPresupuestoSwing() {
        // Configuración de la Ventana Principal
        setTitle("Mantenimiento - PBM_TRIBUTO_PRESUPUESTO (Estructura Real)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 550);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // =========================================================================
        // 1. PANEL DE FORMULARIO (GridBagLayout)
        // =========================================================================
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos del Vínculo Tributo - Presupuesto "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 12, 6, 12);

        txtTpPre = new JTextField(12);
        txtTpTri = new JTextField(12);
        txtTpMonAporte = new JTextField(15);

        // Fila 0: Código Presupuesto
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Código Presupuesto Anual (TpPre):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panelFormulario.add(txtTpPre, gbc);

        // Fila 1: Código Tributo
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Código Tributo Cabecera (TpTri):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panelFormulario.add(txtTpTri, gbc);

        // Fila 2: Monto Aporte
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Monto de Aporte S/ (TpMonAporte):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        panelFormulario.add(txtTpMonAporte, gbc);

        // =========================================================================
        // 2. PANEL DE BOTONES ACCIONES (CRUD)
        // =========================================================================
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Vincular / Insertar");
        JButton btnActualizar = new JButton("Modificar Monto");
        JButton btnEliminar = new JButton("Eliminar (Físico)");
        JButton btnLimpiar = new JButton("Limpiar");

        btnEliminar.setBackground(new Color(255, 200, 200));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        panelSuperior.add(panelFormulario, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);

        // =========================================================================
        // 3. TABLA DE REGISTROS (INNER JOIN Adaptado a tus columnas reales)
        // =========================================================================
        String[] columnas = {
            "ID Presupuesto (TpPre)", "Año Presupuesto", "ID Tributo (TpTri)", "Observación / Detalle Tributo", "Monto Aporte S/", "Fecha Registro"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        panelPrincipal.add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // Procedimiento READ: Listar registros combinados de tu BD
        Runnable cargarDatos = () -> {
            isAjustando = true;
            modeloTabla.setRowCount(0); 
            
            // Query corregida: p.PreAnio y t.TriCod / t.TriObs
            String sql = "SELECT tp.TpPre, p.PreAnio, tp.TpTri, t.TriObs, tp.TpMonAporte, tp.TpFecReg "
                       + "FROM pbm_tributo_presupuesto tp "
                       + "INNER JOIN pbm_presupuesto_anual p ON tp.TpPre = p.PreCod "
                       + "INNER JOIN pat_tributo_cab t ON tp.TpTri = t.TriCod "
                       + "WHERE tp.TpEstReg = '1'";
            
            try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("TpPre"));
                    fila.add(rs.getInt("PreAnio"));
                    fila.add(rs.getInt("TpTri"));
                    fila.add(rs.getString("TriObs") != null ? rs.getString("TriObs") : "Sin observaciones");
                    fila.add(rs.getDouble("TpMonAporte"));
                    fila.add(rs.getDate("TpFecReg"));
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al leer los datos de catastro_db:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false;
            }
        };

        // Carga inicial
        cargarDatos.run();

        // Evento de Selección de Fila
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int filaSel = tablaDatos.getSelectedRow();
            if (filaSel != -1) {
                isAjustando = true;
                txtTpPre.setText(modeloTabla.getValueAt(filaSel, 0).toString());
                txtTpTri.setText(modeloTabla.getValueAt(filaSel, 2).toString());
                txtTpMonAporte.setText(modeloTabla.getValueAt(filaSel, 4).toString());
                
                txtTpPre.setEditable(false);  txtTpPre.setBackground(new Color(240, 240, 240));
                txtTpTri.setEditable(false);  txtTpTri.setBackground(new Color(240, 240, 240));
                isAjustando = false;
            }
        });

        // Acción CREATE
        btnInsertar.addActionListener(e -> {
            String vPre = txtTpPre.getText().trim();
            String vTri = txtTpTri.getText().trim();
            String vMonto = txtTpMonAporte.getText().trim();

            if (vPre.isEmpty() || vTri.isEmpty() || vMonto.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe completar todos los campos del enlace.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                int idPre = Integer.parseInt(vPre);
                int idTri = Integer.parseInt(vTri);
                double monto = Double.parseDouble(vMonto);

                // Corregido con los nombres de PK validados (PreCod y TriCod)
                if (!verificarExistenciaMaestro(conn, "pbm_presupuesto_anual", "PreCod", idPre)) {
                    JOptionPane.showMessageDialog(this, "Inconsistencia: El código de presupuesto '" + idPre + "' no existe.", "Error Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!verificarExistenciaMaestro(conn, "pat_tributo_cab", "TriCod", idTri)) {
                    JOptionPane.showMessageDialog(this, "Inconsistencia: El código de tributo '" + idTri + "' no existe.", "Error Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (verificarDuplicidadPK(conn, idPre, idTri)) {
                    JOptionPane.showMessageDialog(this, "Ya existe una vinculación para esta combinación específica de Presupuesto y Tributo.", "Llave Duplicada", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String sqlInsert = "INSERT INTO pbm_tributo_presupuesto (TpPre, TpTri, TpMonAporte, TpFecReg, TpEstReg) VALUES (?, ?, ?, CURDATE(), '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, idPre);
                    stmt.setInt(2, idTri);
                    stmt.setDouble(3, monto);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "¡Relación registrada de manera exitosa!");
                cargarDatos.run();
                btnLimpiar.doClick();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Los campos de códigos deben ser enteros y el monto un valor numérico.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al insertar registro:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Acción UPDATE
        btnActualizar.addActionListener(e -> {
            if (tablaDatos.getSelectedRow() == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una fila de la tabla para proceder con la actualización.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sqlUpdate = "UPDATE pbm_tributo_presupuesto SET TpMonAporte = ? WHERE TpPre = ? AND TpTri = ?";
            try (Connection conn = conectar(); PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                double nuevoMonto = Double.parseDouble(txtTpMonAporte.getText().trim());
                int idPre = Integer.parseInt(txtTpPre.getText().trim());
                int idTri = Integer.parseInt(txtTpTri.getText().trim());

                stmt.setDouble(1, nuevoMonto);
                stmt.setInt(2, idPre);
                stmt.setInt(3, idTri);

                int afectas = stmt.executeUpdate();
                if (afectas > 0) {
                    JOptionPane.showMessageDialog(this, "Monto modificado exitosamente.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Acción DELETE
        btnEliminar.addActionListener(e -> {
            String vPre = txtTpPre.getText().trim();
            String vTri = txtTpTri.getText().trim();

            if (vPre.isEmpty() || vTri.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro para desvincular.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int seguro = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de eliminar físicamente la vinculación del presupuesto " + vPre + " con el tributo " + vTri + "?", 
                "Confirmación de Borrado Físico", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (seguro != JOptionPane.YES_OPTION) return;

            String sqlDelete = "DELETE FROM pbm_tributo_presupuesto WHERE TpPre = ? AND TpTri = ?";
            try (Connection conn = conectar(); PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, Integer.parseInt(vPre));
                stmt.setInt(2, Integer.parseInt(vTri));

                int filasBorradas = stmt.executeUpdate();
                if (filasBorradas > 0) {
                    JOptionPane.showMessageDialog(this, "Vínculo financiero removido de la base de datos.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró el registro indicado.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al eliminar de la BD:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Resetear interfaz
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtTpPre.setText("");       txtTpPre.setEditable(true);  txtTpPre.setBackground(Color.WHITE);
            txtTpTri.setText("");       txtTpTri.setEditable(true);  txtTpTri.setBackground(Color.WHITE);
            txtTpMonAporte.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private boolean verificarExistenciaMaestro(Connection conn, String tabla, String columnaId, int id) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tabla + " WHERE " + columnaId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean verificarDuplicidadPK(Connection conn, int idPre, int idTri) throws SQLException {
        String query = "SELECT COUNT(*) FROM pbm_tributo_presupuesto WHERE TpPre = ? AND TpTri = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idPre);
            stmt.setInt(2, idTri);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try { Class.forName(DRIVER); } catch (ClassNotFoundException e) { throw new SQLException("Driver MySQL No encontrado."); }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TributoPresupuestoSwing().setVisible(true));
    }
}