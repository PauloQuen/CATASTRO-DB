package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ViviendaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtZon, txtDir, txtUbigeo, txtTipPre, txtUsoPr, txtVal;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public ViviendaSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración modular con CardLayout) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. COMPONENTE FORMULARIO
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de la Vivienda (c3m_vivienda) "));

        txtCod = new JTextField();    // vivcod (VARCHAR(10) - PK)
        txtZon = new JTextField();    // vivzon (INT)
        txtDir = new JTextField();    // vivdir (INT)
        txtUbigeo = new JTextField(); // vivubigeo (VARCHAR(15))
        txtTipPre = new JTextField(); // vivtippre (VARCHAR(4))
        txtUsoPr = new JTextField();  // vivusopre (VARCHAR(4))
        txtVal = new JTextField();    // vivval (NUMERIC(10,2))

        panelFormulario.add(new JLabel("Código Vivienda (vivcod):"));   panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Zona (vivzon - INT):")); panelFormulario.add(txtZon);
        panelFormulario.add(new JLabel("ID Dirección (vivdir - INT):")); panelFormulario.add(txtDir);
        panelFormulario.add(new JLabel("Ubigeo Catastral (vivubigeo):")); panelFormulario.add(txtUbigeo);
        panelFormulario.add(new JLabel("Tipo Predio (vivtippre):"));    panelFormulario.add(txtTipPre);
        panelFormulario.add(new JLabel("Uso Predio (vivusopre):"));     panelFormulario.add(txtUsoPr);
        panelFormulario.add(new JLabel("Valor Autovalúo (vivval):"));   panelFormulario.add(txtVal);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN (Lógica de Estado Alfanumérico puro '1' y '0')
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Predio");
        JButton btnActualizar = new JButton("Modificar Valor");
        JButton btnActivar = new JButton("Activar (1)");
        JButton btnDesactivar = new JButton("Desactivar (0)");
        JButton btnLimpiar = new JButton("Limpiar");

        btnActivar.setBackground(new Color(220, 245, 220));
        btnDesactivar.setBackground(new Color(255, 220, 220));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnActivar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnLimpiar);

        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(pnlSuperiorWrapper, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS VISUALES
        String[] columnas = {"Código", "Zona", "ID Dirección", "Ubigeo", "Tipo Predio", "Uso Predio", "Valor Autovalúo", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA: CARGAR COMPOSICIÓN DE LA TABLA
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT vivcod, vivzon, vivdir, vivubigeo, vivtippre, vivusopre, vivval, vivestreg " +
                         "FROM c3m_vivienda " +
                         "ORDER BY vivestreg DESC, vivcod ASC";
                         
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("vivcod"));
                    fila.add(rs.getInt("vivzon"));
                    fila.add(rs.getObject("vivdir") != null ? rs.getInt("vivdir") : ""); 
                    fila.add(rs.getString("vivubigeo") != null ? rs.getString("vivubigeo") : "");
                    fila.add(rs.getString("vivtippre") != null ? rs.getString("vivtippre") : "");
                    fila.add(rs.getString("vivusopre") != null ? rs.getString("vivusopre") : "");
                    fila.add(rs.getBigDecimal("vivval"));
                    fila.add(rs.getString("vivestreg") != null ? rs.getString("vivestreg").trim() : "");
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al refrescar grilla de catastro: " + ex.getMessage());
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // LISTENER: Pasar selección segura de la tabla a cajas de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                String codigoPredio = modeloTabla.getValueAt(fila, 0).toString();
                String sqlBuscar = "SELECT * FROM c3m_vivienda WHERE vivcod = ?";
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement(sqlBuscar)) {
                    stmt.setString(1, codigoPredio);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            isAjustando = true;
                            txtCod.setText(getValorSeguro(rs.getString("vivcod")));
                            txtZon.setText(getValorSeguro(rs.getInt("vivzon")));
                            txtDir.setText(rs.getObject("vivdir") != null ? getValorSeguro(rs.getInt("vivdir")) : "");
                            txtUbigeo.setText(getValorSeguro(rs.getString("vivubigeo")));
                            txtTipPre.setText(getValorSeguro(rs.getString("vivtippre")));
                            txtUsoPr.setText(getValorSeguro(rs.getString("vivusopre")));
                            txtVal.setText(getValorSeguro(rs.getBigDecimal("vivval")));
                            txtCod.setEditable(false); 
                            isAjustando = false;
                        }
                    }
                } catch (Exception ex) {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (INSERT con Estado por defecto '1' y validación PK)
        btnInsertar.addActionListener(e -> {
            if (camposEstanVacios(true)) return;
            
            int idZona = validarEntero(txtZon.getText(), "Código Zona");
            Integer idDir = txtDir.getText().trim().isEmpty() ? null : validarEntero(txtDir.getText(), "ID Dirección");
            double valorAutovaluo = validarDouble(txtVal.getText(), "Valor Autovalúo");
            
            if (idZona == -1 || (txtDir.getText().trim().length() > 0 && idDir == -1) || valorAutovaluo == -1) return;

            String codigoVivienda = txtCod.getText().trim();

            try (Connection conn = conectar()) {
                // Validación de Clave Primaria Duplicada
                String sqlCheck = "SELECT vivestreg FROM c3m_vivienda WHERE vivcod = ?";
                try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                    stmtCheck.setString(1, codigoVivienda);
                    try (ResultSet rsCheck = stmtCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            JOptionPane.showMessageDialog(this, "El Código de Vivienda ya existe con estado '" + rsCheck.getString("vivestreg").trim() + "'.", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }

                // Inserción exacta mapeada
                String sqlInsert = "INSERT INTO c3m_vivienda (vivcod, vivzon, vivdir, vivubigeo, vivtippre, vivusopre, vivval, vivestreg) VALUES (?, ?, ?, ?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, codigoVivienda);
                    stmt.setInt(2, idZona);
                    if (idDir == null) stmt.setNull(3, Types.INTEGER); else stmt.setInt(3, idDir);
                    stmt.setString(4, txtUbigeo.getText().trim().isEmpty() ? null : txtUbigeo.getText().trim());
                    stmt.setString(5, txtTipPre.getText().trim().isEmpty() ? null : txtTipPre.getText().trim());
                    stmt.setString(6, txtUsoPr.getText().trim().isEmpty() ? null : txtUsoPr.getText().trim());
                    stmt.setDouble(7, valorAutovaluo);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ ¡Ficha de Vivienda catastrada con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL de registro:\n" + ex.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR VALOR CATASTRAL (UPDATE)
        btnActualizar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty() || txtVal.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor seleccione un registro y defina el nuevo Valor.");
                return;
            }

            double nuevoValor = validarDouble(txtVal.getText(), "Valor Autovalúo");
            if (nuevoValor == -1) return;

            String sqlUpdate = "UPDATE c3m_vivienda SET vivval = ? WHERE vivcod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setDouble(1, nuevoValor);
                stmt.setString(2, txtCod.getText().trim());
                
                int filasAfectadas = stmt.executeUpdate();
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "✔ Valor de Autovalúo actualizado correctamente.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN ACTIVAR (Asigna '1')
        btnActivar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una vivienda de la tabla.");
                return;
            }
            cambiarEstadoRegistro(txtCod.getText().trim(), "1");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN DESACTIVAR (Asigna '0')
        btnDesactivar.addActionListener(e -> {
            if (txtCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una vivienda de la tabla.");
                return;
            }
            cambiarEstadoRegistro(txtCod.getText().trim(), "0");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtCod.setText(""); txtZon.setText(""); txtDir.setText("");
            txtUbigeo.setText(""); txtTipPre.setText(""); txtUsoPr.setText(""); txtVal.setText("");
            txtCod.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private void cambiarEstadoRegistro(String codigo, String estado) {
        String sql = "UPDATE c3m_vivienda SET vivestreg = ? WHERE vivcod = ?";
        try (Connection conn = conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setString(2, codigo);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado de vivienda actualizado a: " + estado);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar estado:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int validarEntero(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El '" + nombreCampo + "' debe ser un número entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    private double validarDouble(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El '" + nombreCampo + "' debe ser un valor numérico decimal válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private boolean camposEstanVacios(boolean validarTodo) {
        if (validarTodo) {
            if (txtCod.getText().trim().isEmpty() || txtZon.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Campos primarios obligatorios vacíos: Código Vivienda o Zona.");
                return true;
            }
        }
        if (txtVal.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El valor de autovalúo (vivval) es obligatorio.");
            return true;
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver de PostgreSQL no encontrado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}