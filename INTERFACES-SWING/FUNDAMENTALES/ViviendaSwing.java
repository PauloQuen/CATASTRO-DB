import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ViviendaSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db"
                            + "?useSSL=false&serverTimezone=America/Lima"
                            + "&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASSWORD = "pauloq3408"; 

    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private JTextField txtCod, txtZon, txtDir, txtUbigeo, txtTipPr, txtUsoPr, txtVal;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 

    public ViviendaSwing() {
        setTitle("Módulo de Viviendas (C3M_VIVIENDA)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // 1. COMPONENTE FORMULARIO
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de la Vivienda (C3M_VIVIENDA) "));

        txtCod = new JTextField();    
        txtZon = new JTextField();    
        txtDir = new JTextField();    
        txtUbigeo = new JTextField(); 
        txtTipPr = new JTextField();  
        txtUsoPr = new JTextField();  
        txtVal = new JTextField();    

        panelFormulario.add(new JLabel("Código Vivienda (VivCod):"));  panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Zona (VivZon - INT):")); panelFormulario.add(txtZon);
        panelFormulario.add(new JLabel("ID Dirección (VivDir - INT):")); panelFormulario.add(txtDir);
        panelFormulario.add(new JLabel("Ubigeo Catastral:"));          panelFormulario.add(txtUbigeo);
        panelFormulario.add(new JLabel("Tipo Predio (VivTipPr):"));    panelFormulario.add(txtTipPr);
        panelFormulario.add(new JLabel("Uso Predio (VivUsoPr):"));     panelFormulario.add(txtUsoPr);
        panelFormulario.add(new JLabel("Valor Autovalúo (S/):"));      panelFormulario.add(txtVal);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN (Con interruptores lógicos de Estado Alfanumérico)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Registrar Predio");
        JButton btnActualizar = new JButton("Modificar Valor");
        JButton btnActivar = new JButton("Activar (1)");
        JButton btnDesactivar = new JButton("Desactivar (0)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        btnActivar.setBackground(new Color(220, 245, 220));
        btnDesactivar.setBackground(new Color(255, 220, 220));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnActivar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnLimpiar);

        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS (Muestra el carácter puro en la columna 'Estado')
        String[] columnas = {"Código", "Ubigeo", "Dirección/Calle", "Tipo Predio", "Uso Predio", "Valor Autovalúo (S/)", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        panelPrincipal.add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA: CARGAR COMPOSICIÓN DE LA TABLA
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            // Se quita el filtro restrictivo de VivEstReg='1' para poder ver e interactuar con los inactivos
            String sql = "SELECT v.VivCod, v.VivUbigeo, d.DirViaNom, d.DirNum, tp.TipPrNom, up.UsoPrNom, v.VivVal, v.VivEstReg " +
                         "FROM C3M_VIVIENDA v " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "INNER JOIN c5m_tipo_predio tp ON v.VivTipPr = tp.TipPrCod " +
                         "INNER JOIN c5m_uso_predio up ON v.VivUsoPr = up.UsoPrCod " +
                         "ORDER BY v.VivEstReg DESC, v.VivCod ASC";
                         
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("VivCod"));
                    fila.add(rs.getString("VivUbigeo"));
                    fila.add(rs.getString("DirViaNom") + " #" + rs.getString("DirNum"));
                    fila.add(rs.getString("TipPrNom"));
                    fila.add(rs.getString("UsoPrNom"));
                    fila.add(rs.getDouble("VivVal"));
                    // Recupera el campo alfanumérico limpio de espacios de relleno
                    fila.add(rs.getString("VivEstReg") != null ? rs.getString("VivEstReg").trim() : "");
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al refrescar grilla de catastro: " + ex.getMessage());
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // LISTENER: Fila seleccionada al Formulario
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                String codigoPredio = modeloTabla.getValueAt(fila, 0).toString();
                String sqlBuscar = "SELECT * FROM C3M_VIVIENDA WHERE VivCod = ?";
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement(sqlBuscar)) {
                    stmt.setString(1, codigoPredio);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            isAjustando = true;
                            txtCod.setText(rs.getString("VivCod"));
                            txtZon.setText(String.valueOf(rs.getInt("VivZon")));
                            txtDir.setText(String.valueOf(rs.getInt("VivDir")));
                            txtUbigeo.setText(rs.getString("VivUbigeo"));
                            txtTipPr.setText(rs.getString("VivTipPr"));
                            txtUsoPr.setText(rs.getString("VivUsoPr"));
                            txtVal.setText(String.valueOf(rs.getDouble("VivVal")));
                            txtCod.setEditable(false); 
                            isAjustando = false;
                        }
                    }
                } catch (Exception ex) {
                    isAjustando = false;
                }
            }
        });

        // ACCIÓN: REGISTRAR (INSERT con Estado por defecto '1')
        btnInsertar.addActionListener(e -> {
            if (camposVacios()) return;
            
            try (Connection conn = conectar()) {
                String codigoVivienda = txtCod.getText().trim();
                int idZona = Integer.parseInt(txtZon.getText().trim());
                int idDir = Integer.parseInt(txtDir.getText().trim());
                double valorAutovaluo = Double.parseDouble(txtVal.getText().trim());

                // Validaciones de claves foráneas
                if (!existeId(conn, "c2m_zona", "ZonCod", idZona)) {
                    JOptionPane.showMessageDialog(this, "Error: La Zona ingresada no existe en la DB.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!existeId(conn, "c3m_direccion", "DirCod", idDir)) {
                    JOptionPane.showMessageDialog(this, "Error: El ID Dirección ingresado no existe en la DB.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!existeId(conn, "c5m_tipo_predio", "TipPrCod", txtTipPr.getText().trim())) {
                    JOptionPane.showMessageDialog(this, "Error: El Tipo de Predio no es válido.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!existeId(conn, "c5m_uso_predio", "UsoPrCod", txtUsoPr.getText().trim())) {
                    JOptionPane.showMessageDialog(this, "Error: El Uso de Predio no es válido.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validación de PK Duplicada
                String sqlCheck = "SELECT VivEstReg FROM C3M_VIVIENDA WHERE VivCod = ?";
                try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                    stmtCheck.setString(1, codigoVivienda);
                    try (ResultSet rsCheck = stmtCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            JOptionPane.showMessageDialog(this, "El Código de Vivienda ya existe en el sistema con estado '" + rsCheck.getString("VivEstReg").trim() + "'. Use los botones de estado para cambiarlo.", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }

                // Inserción asignando el valor alfanumérico '1' por defecto
                String sqlInsert = "INSERT INTO C3M_VIVIENDA (VivCod, VivZon, VivDir, VivUbigeo, VivTipPr, VivUsoPr, VivVal, VivEstReg) VALUES (?, ?, ?, ?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, codigoVivienda);
                    stmt.setInt(2, idZona);
                    stmt.setInt(3, idDir);
                    stmt.setString(4, txtUbigeo.getText().trim());
                    stmt.setString(5, txtTipPr.getText().trim());
                    stmt.setString(6, txtUsoPr.getText().trim());
                    stmt.setDouble(7, valorAutovaluo);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ ¡Ficha de Vivienda catastrada con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Los campos Zona, Dirección y Valor deben poseer un formato numérico adecuado.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL de registro:\n" + ex.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: MODIFICAR VALOR CATASTRAL (UPDATE)
        btnActualizar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty() || txtVal.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor seleccione un registro y defina el nuevo Valor Catastral.");
                return;
            }

            String sqlUpdate = "UPDATE C3M_VIVIENDA SET VivVal = ? WHERE VivCod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                double nuevoValor = Double.parseDouble(txtVal.getText().trim());
                stmt.setDouble(1, nuevoValor);
                stmt.setString(2, txtCod.getText().trim());
                
                int filasAfectadas = stmt.executeUpdate();
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "✔ Valor de Autovalúo actualizado correctamente.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "El valor ingresado debe ser numérico.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: BOTÓN ACTIVAR (Asigna la cadena '1')
        btnActivar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una vivienda de la tabla para cambiar su estado a 1.");
                return;
            }
            String codigo = txtCod.getText().trim();
            
            String sql = "UPDATE C3M_VIVIENDA SET VivEstReg = '1' WHERE VivCod = ?";
            try (Connection conn = conectar(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Estado de vivienda actualizado a: 1");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cambiar estado a '1':\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: BOTÓN DESACTIVAR (Asigna la cadena '0')
        btnDesactivar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una vivienda de la tabla para cambiar su estado a 0.");
                return;
            }
            String codigo = txtCod.getText().trim();

            String sql = "UPDATE C3M_VIVIENDA SET VivEstReg = '0' WHERE VivCod = ?";
            try (Connection conn = conectar(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Estado de vivienda actualizado a: 0");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cambiar estado a '0':\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: LIMPIAR CONTROLADORES
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtCod.setText(""); txtZon.setText(""); txtDir.setText("");
            txtUbigeo.setText(""); txtTipPr.setText(""); txtUsoPr.setText(""); txtVal.setText("");
            txtCod.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
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

    private boolean camposVacios() {
        if (txtCod.getText().trim().isEmpty() || txtZon.getText().trim().isEmpty() || 
            txtDir.getText().trim().isEmpty() || txtUbigeo.getText().trim().isEmpty() ||
            txtTipPr.getText().trim().isEmpty() || txtUsoPr.getText().trim().isEmpty() || 
            txtVal.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos de datos de la Vivienda son obligatorios.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ViviendaSwing().setVisible(true);
        });
    }
}