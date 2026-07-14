package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class CasaParticularSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del Formulario (Campos mapeados en minúsculas para PostgreSQL)
    private JTextField txtCasVivCod, txtCasMetC, txtCasOd;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public CasaParticularSwing(JPanel contenedorPadre, String destinoRetorno) {
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

        // 1. PANEL DE FORMULARIO - CAPTURA DE DATOS ESTRUCTURALES
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Especificaciones Técnicas de la Residencia (c3m_casa_particular) "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 12, 6, 12);

        txtCasVivCod = new JTextField(15);
        txtCasMetC = new JTextField(15);
        txtCasOd = new JTextField(35);

        // Distribución en GridBag Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Código de Vivienda Base (casvivcod):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panelFormulario.add(txtCasVivCod, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Metraje Construido / Área m² (casmetc):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panelFormulario.add(txtCasMetC, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Descripción de la Fachada / Interior (casod):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        panelFormulario.add(txtCasOd, gbc);

        // 2. BOTONES DE ACCIÓN (CRUD con enfoque de alteración de estado lógico)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton btnInsertar = new JButton("Registrar Casa");
        JButton btnActualizar = new JButton("Modificar Datos");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Colores temáticos de advertencia y estabilidad de estado administrativo
        btnDesactivar.setBackground(new Color(255, 195, 195));
        btnActivar.setBackground(new Color(195, 230, 195));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnActivar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        panelSuperior.add(pnlSuperiorWrapper, BorderLayout.NORTH);
        panelSuperior.add(panelFormulario, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS CON JOIN RELACIONAL EN POSTGRESQL (Incluye columna de Estado)
        String[] columnas = {
            "Cód. Predio", "Área m² (Const.)", "Descripción Estructural", "Vía Pública", "Nº", "Autovalúo S/", "Estado Registro"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // PROCEDIMIENTO READ (Muestra todos los estados ordenados para auditoría)
        Runnable cargarDatos = () -> {
            isAjustando = true;
            modeloTabla.setRowCount(0);
            String sql = "SELECT cp.casvivcod, cp.casmetc, cp.casod, d.dirvianom, d.dirnum, v.vivval, cp.casestreg "
                       + "FROM c3m_casa_particular cp "
                       + "INNER JOIN c3m_vivienda v ON cp.casvivcod = v.vivcod "
                       + "INNER JOIN c3m_direccion d ON v.vivdir = d.dircod "
                       + "ORDER BY cp.casvivcod ASC";
            try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("casvivcod"));
                    fila.add(rs.getBigDecimal("casmetc"));
                    fila.add(rs.getString("casod") != null ? rs.getString("casod") : "");
                    fila.add(rs.getString("dirvianom"));
                    fila.add(rs.getInt("dirnum"));
                    fila.add(rs.getBigDecimal("vivval"));
                    
                    String estReg = rs.getString("casestreg");
                    fila.add(estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar datos catastrales:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false;
            }
        };

        cargarDatos.run();

        // CONTROL DE EVENTO DE SELECCIÓN EN JTABLE
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int filaSel = tablaDatos.getSelectedRow();
            if (filaSel != -1) {
                isAjustando = true;
                txtCasVivCod.setText(modeloTabla.getValueAt(filaSel, 0).toString());
                txtCasVivCod.setEditable(false); 
                txtCasVivCod.setBackground(new Color(240, 240, 240));
                txtCasMetC.setText(modeloTabla.getValueAt(filaSel, 1).toString());
                txtCasOd.setText(modeloTabla.getValueAt(filaSel, 2).toString());
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE (Manejo preciso con BigDecimal)
        btnInsertar.addActionListener(e -> {
            String codViv = txtCasVivCod.getText().trim();
            String metrajeStr = txtCasMetC.getText().trim();
            String descripcion = txtCasOd.getText().trim();

            if (codViv.isEmpty() || metrajeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Los campos de Código de Vivienda y Metraje son mandatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                if (!existeId(conn, "c3m_vivienda", "vivcod", codViv)) {
                    JOptionPane.showMessageDialog(this, "El código '" + codViv + "' no existe en la tabla base de viviendas (c3m_vivienda).", "Error de Llave Foránea", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (existeId(conn, "c3m_casa_particular", "casvivcod", codViv)) {
                    JOptionPane.showMessageDialog(this, "El predio '" + codViv + "' ya cuenta con una especialización asignada.", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                BigDecimal metraje = new BigDecimal(metrajeStr);

                String insertSql = "INSERT INTO c3m_casa_particular (casvivcod, casmetc, casod, casestreg) VALUES (?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, codViv);
                    stmt.setBigDecimal(2, metraje);
                    stmt.setString(3, descripcion.isEmpty() ? null : descripcion);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ ¡Ficha de Casa Particular guardada con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El formato numérico para el área construida es inválido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE
        btnActualizar.addActionListener(e -> {
            String codViv = txtCasVivCod.getText().trim();
            String metrajeStr = txtCasMetC.getText().trim();
            String descripcion = txtCasOd.getText().trim();

            if (codViv.isEmpty() || metrajeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro de la grilla para aplicar las modificaciones.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE c3m_casa_particular SET casmetc = ?, casod = ? WHERE casvivcod = ?")) {
                
                BigDecimal metraje = new BigDecimal(metrajeStr);
                stmt.setBigDecimal(1, metraje);
                stmt.setString(2, descripcion.isEmpty() ? null : descripcion);
                stmt.setString(3, codViv);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "✔ Características físicas y descriptivas modificadas correctamente.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "No se localizó el registro correspondiente para actualizar.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Verifique el formato decimal del metraje ingresado.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN ESTADO 0: DESACTIVAR REGISTRO (BAJA LÓGICA)
        btnDesactivar.addActionListener(e -> {
            String codViv = txtCasVivCod.getText().trim();
            if (codViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione la Casa Particular de la grilla que desea desactivar administrativamente.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta residencia a inactiva?\n(Se establecerá casestreg = '0')", 
                "Confirmación de Inactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codViv, "0", "El registro especializado ha sido inhabilitado (Desactivado).");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // OPERACIÓN ESTADO 1: ACTIVAR REGISTRO (REACTIVACIÓN LÓGICA)
        btnActivar.addActionListener(e -> {
            String codViv = txtCasVivCod.getText().trim();
            if (codViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una casa inactiva desde el listado inferior para restaurarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codViv, "1", "✔ El registro ha sido reactivado y restaurado al sistema activo con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // REINICIAR CONTROLES
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtCasVivCod.setText("");
            txtCasVivCod.setEditable(true);
            txtCasVivCod.setBackground(Color.WHITE);
            txtCasMetC.setText("");
            txtCasOd.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Método encapsulado para la alteración atómica del Estado de Registro (casestreg)
    private void cambiarEstadoRegistro(String codViv, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c3m_casa_particular SET casestreg = ? WHERE casvivcod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, codViv);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó la clave relacional de la casa en el catastro.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de mutación en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existeId(Connection conn, String tabla, String campoId, String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try { 
            Class.forName(DRIVER); 
        } catch (ClassNotFoundException e) { 
            throw new SQLException("Driver JDBC de PostgreSQL no disponible."); 
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}