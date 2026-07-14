package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Vector;

public class PartidaRegistralSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del Formulario (Campos nativos de la Tabla en PostgreSQL)
    private JTextField txtPrtCod, txtPrtViv, txtPrtZonReg, txtPrtNumPartida;
    private JTextField txtPrtFolioTomo, txtPrtFolioFolio, txtPrtAsiento, txtPrtTipoActo;
    private JTextField txtPrtPropNom, txtPrtPropDNI, txtPrtFecIns, txtPrtFecUltAct, txtPrtAreaM2, txtPrtObs;
    private JComboBox<String> cmbPrtEst; // A, C, D, N

    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false; 

    public PartidaRegistralSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración modular) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. PANEL DE FORMULARIO REORGANIZADO EN DOS COLUMNAS SIMÉTRICAS
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Títulos de Dominio e Inscripción Jurídica (c3m_partida_registral) "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.weightx = 1.0;

        // Sub-panel Izquierdo: Datos de Control e Índices de Ubicación Registral
        JPanel panelIzquierdo = new JPanel(new GridLayout(7, 2, 6, 6));
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder(" Índices y Ubicación Registral (SUNARP) "));
        
        txtPrtCod = new JTextField();
        txtPrtCod.setEditable(false);
        txtPrtCod.setBackground(new Color(240, 240, 240));
        txtPrtViv = new JTextField();
        txtPrtZonReg = new JTextField();
        txtPrtNumPartida = new JTextField();
        txtPrtFolioTomo = new JTextField();
        txtPrtFolioFolio = new JTextField();
        txtPrtAsiento = new JTextField();

        panelIzquierdo.add(new JLabel("ID Interno (prtcod - Serial):")); panelIzquierdo.add(txtPrtCod);
        panelIzquierdo.add(new JLabel("Código Predio (prtviv):")); panelIzquierdo.add(txtPrtViv);
        panelIzquierdo.add(new JLabel("Zona Registral (prtzonreg):")); panelIzquierdo.add(txtPrtZonReg);
        panelIzquierdo.add(new JLabel("Nº Partida Electrónica:")); panelIzquierdo.add(txtPrtNumPartida);
        panelIzquierdo.add(new JLabel("Tomo (prtfoliotomo):")); panelIzquierdo.add(txtPrtFolioTomo);
        panelIzquierdo.add(new JLabel("Folio (prtfoliofolio):")); panelIzquierdo.add(txtPrtFolioFolio);
        panelIzquierdo.add(new JLabel("Asiento Dominio (prtasiento):")); panelIzquierdo.add(txtPrtAsiento);

        // Sub-panel Derecho: Detalles del Acto, Titular y Métricas
        JPanel panelDerecho = new JPanel(new GridLayout(7, 2, 6, 6));
        panelDerecho.setBorder(BorderFactory.createTitledBorder(" Atributos del Acto y Titularidad Legal "));
        
        txtPrtTipoActo = new JTextField();
        txtPrtPropNom = new JTextField();
        txtPrtPropDNI = new JTextField();
        
        cmbPrtEst = new JComboBox<>(new String[]{"A - Activa", "C - Cerrada", "D - Duplicada", "N - Nula"});
        
        txtPrtFecIns = new JTextField();
        txtPrtFecUltAct = new JTextField();
        txtPrtAreaM2 = new JTextField();

        panelDerecho.add(new JLabel("Tipo de Acto (prttipoacto):")); panelDerecho.add(txtPrtTipoActo);
        panelDerecho.add(new JLabel("Nombre Titular (prtpropnom):")); panelDerecho.add(txtPrtPropNom);
        panelDerecho.add(new JLabel("DNI / RUC (prtpropdni):")); panelDerecho.add(txtPrtPropDNI);
        panelDerecho.add(new JLabel("Estado Partida (prtest):")); panelDerecho.add(cmbPrtEst);
        panelDerecho.add(new JLabel("Fec. Inscripción (YYYY-MM-DD):")); panelDerecho.add(txtPrtFecIns);
        panelDerecho.add(new JLabel("Última Act. (YYYY-MM-DD):")); panelDerecho.add(txtPrtFecUltAct);
        panelDerecho.add(new JLabel("Área Registral (m²):")); panelDerecho.add(txtPrtAreaM2);

        // Fila Completa Inferior: Observaciones amplias
        txtPrtObs = new JTextField();
        JPanel panelObs = new JPanel(new BorderLayout(5, 5));
        panelObs.setBorder(BorderFactory.createTitledBorder(" Anotaciones Marginales y Observaciones (prtobs) "));
        panelObs.add(txtPrtObs, BorderLayout.CENTER);

        // Ensamblado del GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; panelFormulario.add(panelIzquierdo, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 1; panelFormulario.add(panelDerecho, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; panelFormulario.add(panelObs, gbc);

        // 2. CONTROLADORES (BOTONES DE ACCIÓN) - Modificados para persistencia de estados
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Inscribir Título");
        JButton btnActualizar = new JButton("Modificar Asiento");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Interfaz");

        // Estilos temáticos para los botones de alteración de estado lógico
        btnDesactivar.setBackground(new Color(255, 195, 195));
        btnActivar.setBackground(new Color(195, 230, 195));

        panelBotones.add(btnInsertar); 
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar); 
        panelBotones.add(btnActivar); 
        panelBotones.add(btnLimpiar);

        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(pnlSuperiorWrapper, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS LEGALES (PostgreSQL - Incluye columna Estado Registro)
        String[] columnas = {
            "ID", "Predio", "Zona R.", "Nº Partida", "Tomo", "Folio", "Asiento", 
            "Tipo Acto", "Propietario", "DNI/RUC", "Est", "Fec Insc", "Fec Act", "Área m²", "Estado Reg."
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // OPERACIÓN READ (Muestra todos los estados ordenados de forma descendente)
        Runnable cargarDatos = () -> {
            isAjustando = true;
            modeloTabla.setRowCount(0);
            String sql = "SELECT prtcod, prtviv, prtzonreg, prtnumpartida, prtfoliotomo, prtfoliofolio, "
                       + "prtasiento, prttipoacto, prtpropnom, prtpropdni, prtest, prtfecins, prtfecultact, prtaream2, prtestreg "
                       + "FROM c3m_partida_registral ORDER BY prtcod DESC";
            try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("prtcod"));
                    fila.add(rs.getString("prtviv"));
                    fila.add(rs.getString("prtzonreg"));
                    fila.add(rs.getString("prtnumpartida"));
                    fila.add(rs.getString("prtfoliotomo") != null ? rs.getString("prtfoliotomo") : "");
                    fila.add(rs.getString("prtfoliofolio") != null ? rs.getString("prtfoliofolio") : "");
                    fila.add(rs.getString("prtasiento") != null ? rs.getString("prtasiento") : "");
                    fila.add(rs.getString("prttipoacto"));
                    fila.add(rs.getString("prtpropnom") != null ? rs.getString("prtpropnom") : "");
                    fila.add(rs.getString("prtpropdni") != null ? rs.getString("prtpropdni") : "");
                    fila.add(rs.getString("prtest") != null ? rs.getString("prtest").trim() : "");
                    fila.add(rs.getDate("prtfecins"));
                    fila.add(rs.getDate("prtfecultact") != null ? rs.getDate("prtfecultact") : "");
                    fila.add(rs.getBigDecimal("prtaream2") != null ? rs.getBigDecimal("prtaream2") : "");
                    
                    String estReg = rs.getString("prtestreg");
                    fila.add(estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al listar partidas catastrales:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false;
            }
        };

        cargarDatos.run();

        // CONTROL DE SELECCIÓN EN TABLA (Mapeo completo al formulario)
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                txtPrtCod.setText(modeloTabla.getValueAt(fila, 0).toString());
                txtPrtViv.setText(modeloTabla.getValueAt(fila, 1).toString());
                txtPrtZonReg.setText(modeloTabla.getValueAt(fila, 2).toString());
                txtPrtNumPartida.setText(modeloTabla.getValueAt(fila, 3).toString());
                txtPrtFolioTomo.setText(modeloTabla.getValueAt(fila, 4).toString());
                txtPrtFolioFolio.setText(modeloTabla.getValueAt(fila, 5).toString());
                txtPrtAsiento.setText(modeloTabla.getValueAt(fila, 6).toString());
                txtPrtTipoActo.setText(modeloTabla.getValueAt(fila, 7).toString());
                txtPrtPropNom.setText(modeloTabla.getValueAt(fila, 8).toString());
                txtPrtPropDNI.setText(modeloTabla.getValueAt(fila, 9).toString());
                
                String est = modeloTabla.getValueAt(fila, 10).toString().trim();
                for (int i = 0; i < cmbPrtEst.getItemCount(); i++) {
                    if (cmbPrtEst.getItemAt(i).startsWith(est)) { cmbPrtEst.setSelectedIndex(i); break; }
                }
                
                txtPrtFecIns.setText(modeloTabla.getValueAt(fila, 11) != null ? modeloTabla.getValueAt(fila, 11).toString() : "");
                txtPrtFecUltAct.setText(modeloTabla.getValueAt(fila, 12) != null ? modeloTabla.getValueAt(fila, 12).toString() : "");
                txtPrtAreaM2.setText(modeloTabla.getValueAt(fila, 13) != null ? modeloTabla.getValueAt(fila, 13).toString() : "");
                
                // Cargar observaciones (prtobs) directo por ID desde PostgreSQL
                try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement("SELECT prtobs FROM c3m_partida_registral WHERE prtcod = ?")) {
                    ps.setInt(1, Integer.parseInt(txtPrtCod.getText()));
                    try (ResultSet rs = ps.executeQuery()) { 
                        if (rs.next() && rs.getString("prtobs") != null) txtPrtObs.setText(rs.getString("prtobs")); else txtPrtObs.setText(""); 
                    }
                } catch (Exception ignored) {}
                
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE
        btnInsertar.addActionListener(e -> {
            try (Connection conn = conectar()) {
                String viv = txtPrtViv.getText().trim();
                String partida = txtPrtNumPartida.getText().trim();
                
                if (viv.isEmpty() || partida.isEmpty() || txtPrtTipoActo.getText().trim().isEmpty() || txtPrtFecIns.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Código Vivienda (prtviv), Partida, Tipo de Acto y Fecha de Inscripción son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Validar Relación con c3m_vivienda (Buscando la PK 'vivcod' exacta)
                if (!existeReferencia(conn, "c3m_vivienda", "vivcod", viv)) {
                    JOptionPane.showMessageDialog(this, "El código de vivienda/predio '" + viv + "' no se encuentra registrado en el sistema catastral.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validar Duplicidad del número de partida
                if (existeReferencia(conn, "c3m_partida_registral", "prtnumpartida", partida)) {
                    JOptionPane.showMessageDialog(this, "La partida electrónica '" + partida + "' ya está inscrita en la base de datos.", "Clave Duplicada", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO c3m_partida_registral(prtviv, prtzonreg, prtnumpartida, prtfoliotomo, prtfoliofolio, prtasiento, prttipoacto, prtpropnom, prtpropdni, prtest, prtfecins, prtfecultact, prtaream2, prtobs, prtestreg) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'1')";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, viv);
                    stmt.setString(2, txtPrtZonReg.getText().trim());
                    stmt.setString(3, partida);
                    stmt.setString(4, txtPrtFolioTomo.getText().trim().isEmpty() ? null : txtPrtFolioTomo.getText().trim());
                    stmt.setString(5, txtPrtFolioFolio.getText().trim().isEmpty() ? null : txtPrtFolioFolio.getText().trim());
                    stmt.setString(6, txtPrtAsiento.getText().trim().isEmpty() ? null : txtPrtAsiento.getText().trim());
                    stmt.setString(7, txtPrtTipoActo.getText().trim());
                    stmt.setString(8, txtPrtPropNom.getText().trim().isEmpty() ? null : txtPrtPropNom.getText().trim());
                    stmt.setString(9, txtPrtPropDNI.getText().trim().isEmpty() ? null : txtPrtPropDNI.getText().trim());
                    stmt.setString(10, cmbPrtEst.getSelectedItem().toString().substring(0, 1));
                    
                    try {
                        stmt.setDate(11, Date.valueOf(txtPrtFecIns.getText().trim()));
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(this, "Formato de Fecha de Inscripción Inválido (Use YYYY-MM-DD)."); return;
                    }

                    if (txtPrtFecUltAct.getText().trim().isEmpty()) {
                        stmt.setNull(12, Types.DATE);
                    } else {
                        try { stmt.setDate(12, Date.valueOf(txtPrtFecUltAct.getText().trim())); } 
                        catch (IllegalArgumentException ex) { JOptionPane.showMessageDialog(this, "Formato de Fecha de Modificación Inválido."); return; }
                    }

                    if (txtPrtAreaM2.getText().trim().isEmpty()) {
                        stmt.setNull(13, Types.DECIMAL);
                    } else {
                        stmt.setBigDecimal(13, new BigDecimal(txtPrtAreaM2.getText().trim()));
                    }
                    
                    stmt.setString(14, txtPrtObs.getText().trim().isEmpty() ? null : txtPrtObs.getText().trim());
                    stmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "✔ ¡Partida Jurídica de Dominio inscrita con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de Datos de Inscripción:\n" + ex.getMessage(), "Error de Registro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE
        btnActualizar.addActionListener(e -> {
            if (txtPrtCod.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un asiento en la tabla para actualizar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (Connection conn = conectar(); 
                 PreparedStatement stmt = conn.prepareStatement("UPDATE c3m_partida_registral SET prtzonreg=?, prtfoliotomo=?, prtfoliofolio=?, prtasiento=?, prttipoacto=?, prtpropnom=?, prtpropdni=?, prtest=?, prtfecins=?, prtfecultact=?, prtaream2=?, prtobs=? WHERE prtcod=?")) {
                
                stmt.setString(1, txtPrtZonReg.getText().trim());
                stmt.setString(2, txtPrtFolioTomo.getText().trim().isEmpty() ? null : txtPrtFolioTomo.getText().trim());
                stmt.setString(3, txtPrtFolioFolio.getText().trim().isEmpty() ? null : txtPrtFolioFolio.getText().trim());
                stmt.setString(4, txtPrtAsiento.getText().trim().isEmpty() ? null : txtPrtAsiento.getText().trim());
                stmt.setString(5, txtPrtTipoActo.getText().trim());
                stmt.setString(6, txtPrtPropNom.getText().trim().isEmpty() ? null : txtPrtPropNom.getText().trim());
                stmt.setString(7, txtPrtPropDNI.getText().trim().isEmpty() ? null : txtPrtPropDNI.getText().trim());
                stmt.setString(8, cmbPrtEst.getSelectedItem().toString().substring(0, 1));
                
                try { stmt.setDate(9, Date.valueOf(txtPrtFecIns.getText().trim())); } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Error en fecha de Inscripción."); return; }
                if (txtPrtFecUltAct.getText().trim().isEmpty()) stmt.setNull(10, Types.DATE); else stmt.setDate(10, Date.valueOf(txtPrtFecUltAct.getText().trim()));
                
                if (txtPrtAreaM2.getText().trim().isEmpty()) {
                    stmt.setNull(11, Types.DECIMAL);
                } else {
                    stmt.setBigDecimal(11, new BigDecimal(txtPrtAreaM2.getText().trim()));
                }
                
                stmt.setString(12, txtPrtObs.getText().trim().isEmpty() ? null : txtPrtObs.getText().trim());
                stmt.setInt(13, Integer.parseInt(txtPrtCod.getText()));
                
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "✔ Asiento registral modificado correctamente.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN ESTADO 0: DESACTIVAR REGISTRO (BAJA LÓGICA)
        btnDesactivar.addActionListener(e -> {
            if (txtPrtCod.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro a dar de baja administrativamente.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta Partida Registral a inactiva?\n(Se establecerá prtestreg = '0')", 
                "Confirmación de Inactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(txtPrtCod.getText(), "0", "La partida registral ha sido inhabilitada (Desactivada) del sistema activo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // OPERACIÓN ESTADO 1: ACTIVAR REGISTRO (REACTIVACIÓN LÓGICA)
        btnActivar.addActionListener(e -> {
            if (txtPrtCod.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una partida inactiva desde el listado para restaurarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(txtPrtCod.getText(), "1", "✔ La partida registral ha sido reactivada y restaurada con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // RESET DE CAMPOS
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtPrtCod.setText(""); txtPrtViv.setText(""); txtPrtZonReg.setText(""); txtPrtNumPartida.setText("");
            txtPrtFolioTomo.setText(""); txtPrtFolioFolio.setText(""); txtPrtAsiento.setText(""); txtPrtTipoActo.setText("");
            txtPrtPropNom.setText(""); txtPrtPropDNI.setText(""); txtPrtFecIns.setText(""); txtPrtFecUltAct.setText("");
            txtPrtAreaM2.setText(""); txtPrtObs.setText(""); cmbPrtEst.setSelectedIndex(0);
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Método encapsulado para la alteración atómica del Estado de Registro (prtestreg)
    private void cambiarEstadoRegistro(String prtCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c3m_partida_registral SET prtestreg = ? WHERE prtcod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, Integer.parseInt(prtCod));
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó la clave interna de la partida en el DBMS.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de mutación en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existeReferencia(Connection conn, String tabla, String campo, String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campo + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try { Class.forName(DRIVER); } catch (ClassNotFoundException e) { throw new SQLException("Driver JDBC de PostgreSQL ausente."); }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}