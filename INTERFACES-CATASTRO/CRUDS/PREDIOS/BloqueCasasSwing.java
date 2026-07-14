package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class BloqueCasasSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del Formulario Gráfico (Campos mapeados en minúsculas)
    private JTextField txtBloVivCod, txtBloMetB, txtBloOd;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false;

    public BloqueCasasSwing(JPanel contenedorPadre, String destinoRetorno) {
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

        // 1. PANEL DE FORMULARIO - CAPTURA DE DATOS TÉCNICOS
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Ficha Técnica de Campo - Bloques de Casas (c3m_bloque_casas) "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 12, 6, 12);

        txtBloVivCod = new JTextField(15);
        txtBloMetB = new JTextField(15);
        txtBloOd = new JTextField(35);

        // Distribución de componentes usando GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Código Vivienda Matriz (blovivcod):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panelFormulario.add(txtBloVivCod, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Metraje Total del Bloque m² (blometb):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panelFormulario.add(txtBloMetB, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panelFormulario.add(new JLabel("Observaciones / Datos Adicionales (blood):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        panelFormulario.add(txtBloOd, gbc);

        // 2. PANEL DE BOTONES DE ACCIÓN (CRUD con enfoque de alteración de estado lógico)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton btnInsertar = new JButton("Registrar Bloque");
        JButton btnActualizar = new JButton("Modificar Atributos");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Colores temáticos de advertencia y estabilidad de estado
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

        // 3. TABLA DE CONTROL CON INNER JOIN RELACIONAL (Muestra todo para auditoría)
        String[] columnas = {
            "Cód. Bloque", "Ubicación Principal", "Urbanización", "Área m² Bloque", "Autovalúo S/", "Notas Estructurales", "Estado Registro"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // OPERACIÓN READ (SELECT CON INCLUSIÓN DE ESTADOS '1' Y '0')
        Runnable cargarDatos = () -> {
            isAjustando = true;
            modeloTabla.setRowCount(0);
            String sql = "SELECT b.blovivcod, d.dirvianom, d.dirnum, d.dirurb, b.blometb, v.vivval, b.blood, b.bloestreg "
                       + "FROM c3m_bloque_casas b "
                       + "INNER JOIN c3m_vivienda v ON b.blovivcod = v.vivcod "
                       + "INNER JOIN c3m_direccion d ON v.vivdir = d.dircod "
                       + "ORDER BY b.blovivcod ASC";
            try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("blovivcod"));
                    fila.add(rs.getString("dirvianom") + " #" + rs.getInt("dirnum"));
                    fila.add(rs.getString("dirurb"));
                    fila.add(rs.getBigDecimal("blometb"));
                    fila.add(rs.getBigDecimal("vivval"));
                    fila.add(rs.getString("blood") != null ? rs.getString("blood") : "");
                    
                    String estReg = rs.getString("bloestreg");
                    fila.add(estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar datos del catastro:\n" + ex.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false;
            }
        };

        cargarDatos.run();

        // CAPTURA DE EVENTOS DE SELECCIÓN DE FILAS DE LA TABLA
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            int filaSel = tablaDatos.getSelectedRow();
            if (filaSel != -1) {
                isAjustando = true;
                txtBloVivCod.setText(modeloTabla.getValueAt(filaSel, 0).toString());
                txtBloVivCod.setEditable(false); 
                txtBloVivCod.setBackground(new Color(240, 240, 240));
                txtBloMetB.setText(modeloTabla.getValueAt(filaSel, 3).toString());
                txtBloOd.setText(modeloTabla.getValueAt(filaSel, 5).toString());
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE
        btnInsertar.addActionListener(e -> {
            String codViv = txtBloVivCod.getText().trim();
            String metajeStr = txtBloMetB.getText().trim();
            String obs = txtBloOd.getText().trim();

            if (codViv.isEmpty() || metajeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Los campos Código de Vivienda y Metraje son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                if (!existeId(conn, "c3m_vivienda", "vivcod", codViv)) {
                    JOptionPane.showMessageDialog(this, "Error de Integridad: El código '" + codViv + "' no está registrado en la tabla base c3m_vivienda.", "Llave Foránea No Encontrada", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (existeId(conn, "c3m_bloque_casas", "blovivcod", codViv)) {
                    JOptionPane.showMessageDialog(this, "Este predio catastral ya se encuentra especializado como un Bloque de Casas.", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                BigDecimal metraje = new BigDecimal(metajeStr);

                String sqlInsert = "INSERT INTO c3m_bloque_casas (blovivcod, blometb, blood, bloestreg) VALUES (?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, codViv);
                    stmt.setBigDecimal(2, metraje);
                    stmt.setString(3, obs.isEmpty() ? null : obs);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ ¡El Bloque de Casas ha sido añadido correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El campo de metraje debe ser un valor numérico decimal válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia:\n" + ex.getMessage(), "Error Interno", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE
        btnActualizar.addActionListener(e -> {
            String codViv = txtBloVivCod.getText().trim();
            String metajeStr = txtBloMetB.getText().trim();
            String obs = txtBloOd.getText().trim();

            if (codViv.isEmpty() || metajeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un bloque desde el listado inferior para modificarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE c3m_bloque_casas SET blometb = ?, blood = ? WHERE blovivcod = ?")) {
                
                BigDecimal metraje = new BigDecimal(metajeStr);
                stmt.setBigDecimal(1, metraje);
                stmt.setString(2, obs.isEmpty() ? null : obs);
                stmt.setString(3, codViv);

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "✔ Los atributos del bloque multifamiliar fueron actualizados con éxito.");
                    cargarDatos.run();
                    btnLimpiar.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró el registro catastral solicitado.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Por favor, corrija el formato del valor numérico del metraje.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de actualización:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN ESTADO 0: DESACTIVAR REGISTRO (BAJA LÓGICA)
        btnDesactivar.addActionListener(e -> {
            String codViv = txtBloVivCod.getText().trim();
            if (codViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar qué Bloque desea desactivar desde el listado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de este bloque a inactivo?\n(Se establecerá bloestreg = '0')", 
                "Confirmación de Inactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codViv, "0", "El registro especializado ha sido inhabilitado (Desactivado).");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // OPERACIÓN ESTADO 1: ACTIVAR REGISTRO (REACTIVACIÓN LÓGICA)
        btnActivar.addActionListener(e -> {
            String codViv = txtBloVivCod.getText().trim();
            if (codViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un bloque de la grilla para restaurar su estado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codViv, "1", "✔ El registro ha sido reactivado y restaurado al sistema activo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // RESETEAR ENTRADAS Y GRILLA
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtBloVivCod.setText("");
            txtBloVivCod.setEditable(true);
            txtBloVivCod.setBackground(Color.WHITE);
            txtBloMetB.setText("");
            txtBloOd.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Método centralizado para el control mutacional del Estado de Registro (bloestreg)
    private void cambiarEstadoRegistro(String codViv, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c3m_bloque_casas SET bloestreg = ? WHERE blovivcod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, codViv);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó la clave relacional asociada en el sistema.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al modificar el estado en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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