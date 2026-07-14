package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class DepartamentoSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes de la interfaz gráfica (Campos mapeados en minúsculas)
    private JTextField txtDepBloCod, txtDepEsc, txtDepNiv, txtDepPue, txtDepMetP, txtDepOd;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false; 

    public DepartamentoSwing(JPanel contenedorPadre, String destinoRetorno) {
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

        // 1. PANEL DE FORMULARIO REORGANIZADO ESTÉRICAMENTE
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Especificaciones Técnicas del Departamento (c4m_departamento) "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 12, 6, 12); 
        gbc.weightx = 1.0;

        // Sub-panel Izquierdo: Ubicación y Clave Compuesta
        JPanel panelIzquierdo = new JPanel(new GridLayout(4, 2, 8, 8));
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder(" Identificación Única (Ubicación Estructural) "));
        txtDepBloCod = new JTextField();
        txtDepEsc = new JTextField();       
        txtDepNiv = new JTextField();       
        txtDepPue = new JTextField();       
        
        panelIzquierdo.add(new JLabel("Código Bloque/VIV (depblocod):")); panelIzquierdo.add(txtDepBloCod);
        panelIzquierdo.add(new JLabel("Escalera / Sector (depesc):"));     panelIzquierdo.add(txtDepEsc);
        panelIzquierdo.add(new JLabel("Número Piso / Nivel (depniv):"));  panelIzquierdo.add(txtDepNiv);
        panelIzquierdo.add(new JLabel("Puerta / Departamento (deppue):"));panelIzquierdo.add(txtDepPue);

        // Sub-panel Derecho: Métricas y Descripción
        JPanel panelDerecho = new JPanel(new GridLayout(4, 2, 8, 8)); 
        panelDerecho.setBorder(BorderFactory.createTitledBorder(" Características de la Unidad Privativa "));
        txtDepMetP = new JTextField();       
        txtDepOd = new JTextField();       
        
        panelDerecho.add(new JLabel("Metraje Privativo m² (depmetp):")); panelDerecho.add(txtDepMetP);
        panelDerecho.add(new JLabel("Observaciones (depod):"));         panelDerecho.add(txtDepOd);
        panelDerecho.add(new JLabel("")); panelDerecho.add(new JLabel(""));
        panelDerecho.add(new JLabel("")); panelDerecho.add(new JLabel(""));

        gbc.gridx = 0; gbc.gridy = 0;
        panelFormulario.add(panelIzquierdo, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        panelFormulario.add(panelDerecho, gbc);

        // 2. CONTROLADORES DE ACCIONES (BOTONES CON ENFOQUE DE BAJA/ALTA LÓGICA)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton btnInsertar = new JButton("Registrar Unidad");
        JButton btnActualizar = new JButton("Modificar Datos");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Colores distintivos de advertencia y confirmación para estados de registro
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

        // 3. TABLA DE REGISTROS CATASTRALES (Con columna de visualización de Estado)
        String[] columnas = {
            "Código Bloque", "Escalera", "Piso", "Nº Puerta", "Metraje (m²)", "Descripción / Observaciones", "Estado Registro"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // OPERACIÓN READ (Muestra registros activos '1' y dados de baja '0' para auditoría interna)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT depblocod, depesc, depniv, deppue, depmetp, depod, depestreg "
                       + "FROM c4m_departamento "
                       + "ORDER BY depblocod ASC, depesc ASC, depniv ASC, deppue ASC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("depblocod"));
                    fila.add(rs.getString("depesc"));
                    fila.add(rs.getInt("depniv"));
                    fila.add(rs.getString("deppue"));
                    fila.add(rs.getBigDecimal("depmetp"));
                    fila.add(rs.getString("depod") != null ? rs.getString("depod") : "");
                    
                    String estReg = rs.getString("depestreg");
                    fila.add(estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al procesar el listado de departamentos:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // CONTROL DE SELECCIÓN EN TABLA (Mapeo a campos e inhabilitación de claves compuestas)
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                txtDepBloCod.setText(modeloTabla.getValueAt(fila, 0).toString());
                txtDepEsc.setText(modeloTabla.getValueAt(fila, 1).toString());
                txtDepNiv.setText(modeloTabla.getValueAt(fila, 2).toString());
                txtDepPue.setText(modeloTabla.getValueAt(fila, 3).toString());
                txtDepMetP.setText(modeloTabla.getValueAt(fila, 4).toString());
                txtDepOd.setText(modeloTabla.getValueAt(fila, 5).toString());

                txtDepBloCod.setEditable(false);
                txtDepEsc.setEditable(false);
                txtDepNiv.setEditable(false);
                txtDepPue.setEditable(false);
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE: REGISTRAR DEPARTAMENTO INTERNO
        btnInsertar.addActionListener(e -> {
            String strBloCod = txtDepBloCod.getText().trim();
            String strEsc = txtDepEsc.getText().trim();
            String strNiv = txtDepNiv.getText().trim();
            String strPue = txtDepPue.getText().trim();
            String strMetP = txtDepMetP.getText().trim();
            String strOd = txtDepOd.getText().trim();

            if (strBloCod.isEmpty() || strEsc.isEmpty() || strNiv.isEmpty() || strPue.isEmpty() || strMetP.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos de ubicación física y metraje son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                short depNiv = Short.parseShort(strNiv);
                BigDecimal depMetP = new BigDecimal(strMetP);

                // Validar Integridad con el Catastro Relacional Padre (c3m_vivienda)
                if (!existeId(conn, "c3m_vivienda", "vivcod", strBloCod)) {
                    JOptionPane.showMessageDialog(this, "El código catastral '" + strBloCod + "' no se encuentra registrado en la tabla base c3m_vivienda.", "Violación de Integridad FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Validar colisiones por Llave Primaria Compuesta Estricta
                if (existeClaveCompuesta(conn, strBloCod, strEsc, depNiv, strPue)) {
                    JOptionPane.showMessageDialog(this, "Conflicto: Ya existe un registro para este Bloque, Escalera, Nivel y Puerta.", "Clave Duplicada", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String sqlInsert = "INSERT INTO c4m_departamento (depblocod, depesc, depniv, deppue, depmetp, depod, depestreg) VALUES (?, ?, ?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, strBloCod);
                    stmt.setString(2, strEsc);
                    stmt.setShort(3, depNiv);
                    stmt.setString(4, strPue);
                    stmt.setBigDecimal(5, depMetP);
                    stmt.setString(6, strOd.isEmpty() ? null : strOd);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ Sub-unidad inmobiliaria registrada con éxito.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Verifique los campos numéricos:\n- Piso debe ser un número entero corto.\n- Metraje debe ser un valor decimal válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de Persistencia:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE: MODIFICAR METRAJE Y OBSERVACIONES
        btnActualizar.addActionListener(e -> {
            String strBloCod = txtDepBloCod.getText().trim();
            String strEsc = txtDepEsc.getText().trim();
            String strNiv = txtDepNiv.getText().trim();
            String strPue = txtDepPue.getText().trim();
            String strMetP = txtDepMetP.getText().trim();
            String strOd = txtDepOd.getText().trim();

            if (strBloCod.isEmpty() || strEsc.isEmpty() || strNiv.isEmpty() || strPue.isEmpty() || strMetP.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una sub-unidad de la tabla para realizar modificaciones.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                short depNiv = Short.parseShort(strNiv);
                BigDecimal nuevoMetraje = new BigDecimal(strMetP);

                String sqlUpdate = "UPDATE c4m_departamento SET depmetp = ?, depod = ? WHERE depblocod = ? AND depesc = ? AND depniv = ? AND deppue = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setBigDecimal(1, nuevoMetraje);
                    stmt.setString(2, strOd.isEmpty() ? null : strOd);
                    stmt.setString(3, strBloCod);
                    stmt.setString(4, strEsc);
                    stmt.setShort(5, depNiv);
                    stmt.setString(6, strPue);
                    
                    int affected = stmt.executeUpdate();
                    if (affected > 0) {
                        JOptionPane.showMessageDialog(this, "✔ Dimensiones y observaciones actualizadas correctamente.");
                        cargarDatos.run();
                        btnLimpiar.doClick();
                    } else {
                        JOptionPane.showMessageDialog(this, "No se localizó el registro estructural indicado.", "Error de Referencia", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El metraje ingresado debe corresponder a un formato numérico adecuado.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN ESTADO 0: DESACTIVAR REGISTRO (BAJA LÓGICA)
        btnDesactivar.addActionListener(e -> {
            String strBloCod = txtDepBloCod.getText().trim();
            String strEsc = txtDepEsc.getText().trim();
            String strNiv = txtDepNiv.getText().trim();
            String strPue = txtDepPue.getText().trim();

            if (strBloCod.isEmpty() || strEsc.isEmpty() || strNiv.isEmpty() || strPue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un departamento para cambiar su estado a inactivo.", "Campos Requeridos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirmar = JOptionPane.showConfirmDialog(this, 
                "¿Desea dar de baja administrativamente este departamento?\n(Se establecerá depestreg = '0')", 
                "Confirmación de Inactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirmar != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(strBloCod, strEsc, strNiv, strPue, "0", "El registro ha sido inhabilitado (Desactivado).");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // OPERACIÓN ESTADO 1: ACTIVAR REGISTRO (REACTIVACIÓN LÓGICA)
        btnActivar.addActionListener(e -> {
            String strBloCod = txtDepBloCod.getText().trim();
            String strEsc = txtDepEsc.getText().trim();
            String strNiv = txtDepNiv.getText().trim();
            String strPue = txtDepPue.getText().trim();

            if (strBloCod.isEmpty() || strEsc.isEmpty() || strNiv.isEmpty() || strPue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un departamento de la grilla para reactivarlo.", "Campos Requeridos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(strBloCod, strEsc, strNiv, strPue, "1", "✔ El registro ha sido restaurado al estado activo correctamente.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // REINICIAR Y LIMPIAR EL FORMULARIO
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtDepBloCod.setText("");
            txtDepEsc.setText("");
            txtDepNiv.setText("");
            txtDepPue.setText("");
            txtDepMetP.setText("");
            txtDepOd.setText("");
            
            txtDepBloCod.setEditable(true);
            txtDepEsc.setEditable(true);
            txtDepNiv.setEditable(true);
            txtDepPue.setEditable(true);
            
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Método centralizado para el control transaccional del Estado de Registro (depestreg)
    private void cambiarEstadoRegistro(String blo, String esc, String niv, String pue, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c4m_departamento SET depestreg = ? WHERE depblocod = ? AND depesc = ? AND depniv = ? AND deppue = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, blo);
            stmt.setString(3, esc);
            stmt.setShort(4, Short.parseShort(niv));
            stmt.setString(5, pue);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó la clave relacional compuesta en la base de datos.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de control de estado en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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

    private boolean existeClaveCompuesta(Connection conn, String blo, String esc, short niv, String pue) throws SQLException {
        String sql = "SELECT COUNT(*) FROM c4m_departamento WHERE depblocod = ? AND depesc = ? AND depniv = ? AND deppue = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, blo);
            stmt.setString(2, esc);
            stmt.setShort(3, niv);
            stmt.setString(4, pue);
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