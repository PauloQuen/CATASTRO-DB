package CRUDS.CONTRIBUYENTES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class CabFamiliaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del formulario h6m_cab_familia
    private JTextField txtCabDni, txtMieDni, txtCabFecIni, txtCabFecFin;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 
    private Container principalContainer;
    private String vistaOrigen;

    public CabFamiliaSwing(Container container, String vistaOrigen) {
        this.principalContainer = container;
        this.vistaOrigen = vistaOrigen;

        // Configuración del Layout del Panel propio
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 0. CONTENEDOR SUPERIOR PARA EL BOTÓN DE REGRESO (Alineado a la izquierda)
        JPanel panelNavegacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JButton btnVolver = new JButton("← Volver al Menú de Contribuyentes");
        btnVolver.setBackground(new Color(245, 245, 245));
        panelNavegacion.add(btnVolver);

        // 1. FORMULARIO DE DATOS
        JPanel panelFormulario = new JPanel(new GridLayout(3, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos del Vínculo Familiar (Clave Compuesta) "));

        txtCabDni = new JTextField();       
        txtMieDni = new JTextField();       
        txtCabFecIni = new JTextField();    
        txtCabFecFin = new JTextField();    
        txtCabFecFin.setEditable(false); // Solo se altera mediante el flujo o botón concluir
        txtCabFecFin.setBackground(new Color(240, 240, 240));

        panelFormulario.add(new JLabel("DNI Cabeza Familia (FK):")); panelFormulario.add(txtCabDni);
        panelFormulario.add(new JLabel("DNI Miembro Familiar (FK):")); panelFormulario.add(txtMieDni);
        panelFormulario.add(new JLabel("Fecha Inicio (YYYY-MM-DD):")); panelFormulario.add(txtCabFecIni);
        panelFormulario.add(new JLabel("Fecha Fin / Conclusión:"));     panelFormulario.add(txtCabFecFin);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Vincular Miembro");
        JButton btnConcluir = new JButton("Concluir Vínculo");
        JButton btnActivar = new JButton("Activar (1)");
        JButton btnDesactivar = new JButton("Desactivar (0)");
        JButton btnLimpiar = new JButton("Limpiar");

        btnActivar.setBackground(new Color(220, 245, 220));
        btnDesactivar.setBackground(new Color(255, 220, 220));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnConcluir);
        panelBotones.add(btnActivar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnLimpiar);

        // Armar el bloque Norte
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(panelNavegacion, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS CON DOBLE INNER JOIN HISTÓRICO (PostgreSQL)
        String[] columnas = {
            "DNI Cabeza", "Cabeza de Familia", "DNI Miembro", "Miembro Familiar", "Fec Inicio", "Fec Fin", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // HILO DE LECTURA (READ)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            // Concatenación estándar compatible con PostgreSQL (||)
            String sql = "SELECT r.CabDNI, (c.PerApePat || ' ' || c.PerApeMat || ', ' || c.PerNom) AS NombreCabeza, "
                       + "r.MieDNI, (m.PerApePat || ' ' || m.PerApeMat || ', ' || m.PerNom) AS NombreMiembro, "
                       + "r.CabFecIni, r.CabFecFin, r.CabEstReg "
                       + "FROM h6m_cab_familia r "
                       + "INNER JOIN h6m_persona c ON r.CabDNI = c.PerDNI "
                       + "INNER JOIN h6m_persona m ON r.MieDNI = m.PerDNI "
                       + "ORDER BY r.CabEstReg DESC, r.CabFecIni DESC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(String.format("%08d", rs.getInt("CabDNI")));
                    fila.add(rs.getString("NombreCabeza") != null ? rs.getString("NombreCabeza").trim() : "");
                    fila.add(String.format("%08d", rs.getInt("MieDNI")));
                    fila.add(rs.getString("NombreMiembro") != null ? rs.getString("NombreMiembro").trim() : "");
                    fila.add(rs.getDate("CabFecIni"));
                    
                    Date fFin = rs.getDate("CabFecFin");
                    fila.add(fFin == null ? "VIGENTE" : fFin.toString());
                    fila.add(rs.getString("CabEstReg") != null ? rs.getString("CabEstReg").trim() : "");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar relaciones familiares:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        // Retardo para carga fluida inicial
        Timer timer = new Timer(200, e -> cargarDatos.run());
        timer.setRepeats(false);
        timer.start();

        // PASAR REGISTRO SELECCIONADO A LOS CAMPOS
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                int cabDniSel = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
                int mieDniSel = Integer.parseInt(modeloTabla.getValueAt(fila, 2).toString());
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM h6m_cab_familia WHERE CabDNI = ? AND MieDNI = ?")) {
                    stmt.setInt(1, cabDniSel);
                    stmt.setInt(2, mieDniSel);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            txtCabDni.setText(String.format("%08d", rs.getInt("CabDNI")));
                            txtMieDni.setText(String.format("%08d", rs.getInt("MieDNI")));
                            txtCabFecIni.setText(rs.getDate("CabFecIni").toString());
                            
                            Date fFin = rs.getDate("CabFecFin");
                            txtCabFecFin.setText(fFin == null ? "" : fFin.toString());
                            
                            // Bloquear PK compuesta en edición para proteger integridad referencial
                            txtCabDni.setEditable(false);
                            txtMieDni.setEditable(false);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Error al recuperar selección: " + ex.getMessage());
                } finally {
                    isAjustando = false;
                }
            }
        });

        // ACCIÓN: REGISTRAR VÍNCULO (CREATE)
        btnInsertar.addActionListener(e -> {
            if (camposEstanVacios()) return;
            
            if (!txtCabDni.getText().trim().matches("^\\d{1,8}$") || !txtMieDni.getText().trim().matches("^\\d{1,8}$")) {
                JOptionPane.showMessageDialog(this, "Los DNIs deben ser numéricos de hasta 8 dígitos.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int cabDni = Integer.parseInt(txtCabDni.getText().trim());
            int mieDni = Integer.parseInt(txtMieDni.getText().trim());
            String fecIni = txtCabFecIni.getText().trim();
            
            try (Connection conn = conectar()) {
                // 1. Validar integridad de llaves en h6m_persona (Tratamiento numérico estricto para evitar errores lower(integer))
                if (!existeId(conn, "h6m_persona", "PerDNI", cabDni)) {
                    JOptionPane.showMessageDialog(this, "El DNI de la cabeza de familia no existe en h6m_persona.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!existeId(conn, "h6m_persona", "PerDNI", mieDni)) {
                    JOptionPane.showMessageDialog(this, "El DNI del miembro no existe en h6m_persona.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // 2. Evitar colisión de Clave Primaria Compuesta
                if (existeRelacion(conn, cabDni, mieDni)) {
                    JOptionPane.showMessageDialog(this, "Este vínculo ya está registrado en el sistema.", "Clave Duplicada", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sqlInsert = "INSERT INTO h6m_cab_familia (CabDNI, MieDNI, CabFecIni, CabFecFin, CabEstReg) VALUES (?, ?, ?, NULL, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, cabDni);
                    stmt.setInt(2, mieDni);
                    stmt.setDate(3, Date.valueOf(fecIni));
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Miembro familiar vinculado exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha incorrecto. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: CONCLUIR VÍNCULO (UPDATE DE FECHA FIN)
        btnConcluir.addActionListener(e -> {
            if (txtCabDni.getText().trim().isEmpty() || txtMieDni.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro de la tabla para concluir la relación.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int cabDni = Integer.parseInt(txtCabDni.getText().trim());
            int mieDni = Integer.parseInt(txtMieDni.getText().trim());
            
            String fecFin = JOptionPane.showInputDialog(this, "Ingrese la Fecha de Fin / Salida del hogar (YYYY-MM-DD):");
            if (fecFin == null || fecFin.trim().isEmpty()) return;
            
            String sqlUpdate = "UPDATE h6m_cab_familia SET CabFecFin = ? WHERE CabDNI = ? AND MieDNI = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setDate(1, Date.valueOf(fecFin.trim()));
                stmt.setInt(2, cabDni);
                stmt.setInt(3, mieDni);
                
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "¡Vínculo familiar concluido correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha incorrecto. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al actualizar conclusión:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: ACTIVACIÓN LÓGICA (1)
        btnActivar.addActionListener(e -> {
            if (txtCabDni.getText().trim().isEmpty() || txtMieDni.getText().trim().isEmpty()) return;
            cambiarEstado(Integer.parseInt(txtCabDni.getText().trim()), Integer.parseInt(txtMieDni.getText().trim()), "1");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: DESACTIVACIÓN LÓGICA (0)
        btnDesactivar.addActionListener(e -> {
            if (txtCabDni.getText().trim().isEmpty() || txtMieDni.getText().trim().isEmpty()) return;
            cambiarEstado(Integer.parseInt(txtCabDni.getText().trim()), Integer.parseInt(txtMieDni.getText().trim()), "0");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: LIMPIAR FORMULARIO
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCabDni.setText(""); txtMieDni.setText(""); 
            txtCabFecIni.setText(""); txtCabFecFin.setText(""); 
            txtCabDni.setEditable(true); 
            txtMieDni.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });

        // ACCIÓN: REGRESAR AL MENÚ ANTERIOR
        btnVolver.addActionListener(e -> {
            if (principalContainer != null && principalContainer.getLayout() instanceof CardLayout) {
                btnLimpiar.doClick(); 
                CardLayout cl = (CardLayout) principalContainer.getLayout();
                cl.show(principalContainer, vistaOrigen); 
            } else {
                Component topAncestor = SwingUtilities.getWindowAncestor(this);
                if (topAncestor instanceof JFrame) {
                    ((JFrame) topAncestor).dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró el contenedor de navegación principal.", "Error de Navegación", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void cambiarEstado(int cabDni, int mieDni, String estado) {
        String sql = "UPDATE h6m_cab_familia SET CabEstReg = ? WHERE CabDNI = ? AND MieDNI = ?";
        try (Connection conn = conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setInt(2, cabDni);
            stmt.setInt(3, mieDni);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado del vínculo actualizado a: " + estado);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar estado:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existeId(Connection conn, String tabla, String campoId, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean existeRelacion(Connection conn, int cabDni, int mieDni) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h6m_cab_familia WHERE CabDNI = ? AND MieDNI = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cabDni);
            stmt.setInt(2, mieDni);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean camposEstanVacios() {
        if (txtCabDni.getText().trim().isEmpty() || txtMieDni.getText().trim().isEmpty() || 
            txtCabFecIni.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos DNI Cabeza, DNI Miembro y Fecha de Inicio son mandatorios.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println("Nota: Driver no cargado por Class.forName, intentando resolución nativa JDBC...");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}