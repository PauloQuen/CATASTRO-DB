package CRUDS.CONTRIBUYENTES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PropietarioSwing extends JPanel {

    // Manteniendo la base de datos tal cual está en tu pgAdmin
    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del Formulario
    private JTextField txtProCod, txtProPer, txtProViv, txtProEscCod, txtProEscVig, txtProFecCla;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 
    private JPanel contenedorPadre;
    private String destinoRetorno;

    // Constructor adaptado perfectamente para navegación por CardLayout
    public PropietarioSwing(JPanel contenedorPadre, String destinoRetorno) {
        this.contenedorPadre = contenedorPadre;
        this.destinoRetorno = destinoRetorno;

        // Configuración del contenedor base (JPanel)
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JButton btnVolver = new JButton("← Volver al Menú de Contribuyentes");
        btnVolver.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) this.contenedorPadre.getLayout();
            layout.show(this.contenedorPadre, this.destinoRetorno);
        });

        JPanel panelSuperiorNavegacion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSuperiorNavegacion.add(btnVolver);

        // 1. FORMULARIO DE DATOS
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos del Expediente de Propiedad / Clasificación (H8M_PROPIETARIO) "));

        txtProCod = new JTextField();
        txtProCod.setEditable(false); 
        txtProCod.setBackground(new Color(240, 240, 240));
        
        txtProPer = new JTextField();       
        txtProViv = new JTextField();       
        txtProEscCod = new JTextField();    
        txtProEscVig = new JTextField();    
        txtProFecCla = new JTextField();    

        panelFormulario.add(new JLabel("Código Propietario (Autonumérico):")); panelFormulario.add(txtProCod);
        panelFormulario.add(new JLabel("DNI del Contribuyente (FK Persona):")); panelFormulario.add(txtProPer);
        panelFormulario.add(new JLabel("Predio Principal (FK Vivienda - Opcional):")); panelFormulario.add(txtProViv);
        panelFormulario.add(new JLabel("Código de Escala (Opcional - Ej: E01):")); panelFormulario.add(txtProEscCod);
        panelFormulario.add(new JLabel("Año Vigencia Escala (Opcional - Ej: 2026):")); panelFormulario.add(txtProEscVig);
        panelFormulario.add(new JLabel("Fecha Clasificación (YYYY-MM-DD):")); panelFormulario.add(txtProFecCla);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Registrar Propietario");
        JButton btnActualizar = new JButton("Reclasificar Escala");
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
        panelNorte.add(panelSuperiorNavegacion, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS CON INNER JOINS ADAPTADO A POSTGRESQL
        String[] columnas = {
            "Código", "DNI Contribuyente", "Nombre Ciudadano", "Predio Principal", "Escala", "Año Vig.", "Fec. Clasificación", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);
        
        // HILO DE LECTURA (READ - PADRÓN GENERAL)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            // PostgreSQL utiliza minúsculas por defecto en identificadores sin comillas
            String sql = "SELECT pr.procod, pr.proper, (p.perapepat || ' ' || p.perapemat || ', ' || p.pernom) AS ciudadano, "
                       + "pr.proviv, pr.proesccod, pr.proescvig, pr.profeccla, pr.proestreg "
                       + "FROM public.h8m_propietario pr "
                       + "INNER JOIN public.h6m_persona p ON pr.proper = p.perdni "
                       + "ORDER BY pr.proestreg DESC, pr.procod DESC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("procod"));
                    fila.add(String.format("%08d", rs.getInt("proper")));
                    fila.add(rs.getString("ciudadano"));
                    
                    String viv = rs.getString("proviv");
                    fila.add(viv != null ? viv : "SIN ASIGNAR");
                    
                    String esc = rs.getString("proesccod");
                    fila.add(esc != null ? esc : "NINGUNA");
                    
                    int vig = rs.getInt("proescvig");
                    fila.add(rs.wasNull() ? "-" : vig);
                    
                    Date fCla = rs.getDate("profeccla");
                    fila.add(fCla == null ? "-" : fCla.toString());
                    fila.add(rs.getString("proestreg") != null ? rs.getString("proestreg").trim() : "1");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al mapear padrón de propietarios:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        // Delay fluido usando Timer para la carga inicial
        Timer timer = new Timer(200, e -> cargarDatos.run());
        timer.setRepeats(false);
        timer.start();

        // ACCIÓN AL SELECCIONAR FILA DE LA TABLA
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                int proCodSel = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM public.h8m_propietario WHERE procod = ?")) {
                    stmt.setInt(1, proCodSel);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            txtProCod.setText(String.valueOf(rs.getInt("procod")));
                            txtProPer.setText(String.format("%08d", rs.getInt("proper")));
                            
                            String viv = rs.getString("proviv");
                            txtProViv.setText(viv == null ? "" : viv);
                            
                            String escCod = rs.getString("proesccod");
                            txtProEscCod.setText(escCod == null ? "" : escCod);
                            
                            int escVig = rs.getInt("proescvig");
                            txtProEscVig.setText(rs.wasNull() ? "" : String.valueOf(escVig));
                            
                            Date fCla = rs.getDate("profeccla");
                            txtProFecCla.setText(fCla == null ? "" : fCla.toString());
                            
                            // Proteger integridad inalterable de claves foráneas primarias en edición
                            txtProPer.setEditable(false);
                            txtProViv.setEditable(false);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Error al recuperar expediente: " + ex.getMessage());
                } finally {
                    isAjustando = false;
                }
            }
        });

        // ACCIÓN: REGISTRAR NUEVO PROPIETARIO (CREATE)
        btnInsertar.addActionListener(e -> {
            if (txtProPer.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El campo DNI del Contribuyente es mandatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try (Connection conn = conectar()) {
                int proPer = Integer.parseInt(txtProPer.getText().trim());
                
                // Validación contra h6m_persona en PostgreSQL
                if (!existeId(conn, "public.h6m_persona", "perdni", proPer)) {
                    JOptionPane.showMessageDialog(this, "El DNI ingresado no está registrado en el maestro H6M_PERSONA.", "Violación de FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String proViv = txtProViv.getText().trim();
                if (!proViv.isEmpty() && !existeId(conn, "public.c3m_vivienda", "vivcod", proViv)) {
                    JOptionPane.showMessageDialog(this, "El código de vivienda especificado no existe en C3M_VIVIENDA.", "Violación de FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String proEscCod = txtProEscCod.getText().trim();
                Integer proEscVig = null;
                String proFecCla = txtProFecCla.getText().trim();
                
                if (!proEscCod.isEmpty()) {
                    if (txtProEscVig.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Si define una Escala, debe indicar su Año de Vigencia.", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    proEscVig = Integer.parseInt(txtProEscVig.getText().trim());
                    
                    if (!existeEscala(conn, proEscCod, proEscVig)) {
                        JOptionPane.showMessageDialog(this, "La escala " + proEscCod + " para el año " + proEscVig + " no existe.", "Error Referencial", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (proFecCla.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Debe especificar la Fecha de Clasificación (YYYY-MM-DD).", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                String sqlInsert = "INSERT INTO public.h8m_propietario (proper, proviv, proesccod, proescvig, profeccla, proestreg) VALUES (?, ?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, proPer);
                    
                    if (proViv.isEmpty()) stmt.setNull(2, Types.VARCHAR);
                    else stmt.setString(2, proViv);
                    
                    if (proEscCod.isEmpty()) {
                        stmt.setNull(3, Types.VARCHAR);
                        stmt.setNull(4, Types.INTEGER);
                        stmt.setNull(5, Types.DATE);
                    } else {
                        stmt.setString(3, proEscCod);
                        stmt.setInt(4, proEscVig);
                        stmt.setDate(5, Date.valueOf(proFecCla));
                    }
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Contribuyente incorporado al padrón de propietarios exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha incorrecto. Utilice YYYY-MM-DD.", "Error de Tipado", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al insertar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: ACTUALIZAR ESCALA / RECLASIFICAR (UPDATE)
        btnActualizar.addActionListener(e -> {
            if (txtProCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un propietario de la lista para proceder con la reclasificación.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String proEscCod = txtProEscCod.getText().trim();
            if (proEscCod.isEmpty() || txtProEscVig.getText().trim().isEmpty() || txtProFecCla.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Para actualizar la escala, complete los campos: Código de Escala, Año Vigencia y Fecha Clasificación.", "Campos Mandatorios", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int proCod = Integer.parseInt(txtProCod.getText().trim());
            int proEscVig = Integer.parseInt(txtProEscVig.getText().trim());
            String proFecCla = txtProFecCla.getText().trim();
            
            try (Connection conn = conectar()) {
                if (!existeEscala(conn, proEscCod, proEscVig)) {
                    JOptionPane.showMessageDialog(this, "La escala configurada no existe en la matriz tributaria anual.", "Error Referencial", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String sqlUpdate = "UPDATE public.h8m_propietario SET proesccod = ?, proescvig = ?, profeccla = ? WHERE procod = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setString(1, proEscCod);
                    stmt.setInt(2, proEscVig);
                    stmt.setDate(3, Date.valueOf(proFecCla));
                    stmt.setInt(4, proCod);
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Escala tributaria del propietario actualizada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha incorrecto (YYYY-MM-DD).", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: ACTIVACIÓN LÓGICA (1)
        btnActivar.addActionListener(e -> {
            if (txtProCod.getText().trim().isEmpty()) return;
            cambiarEstado(Integer.parseInt(txtProCod.getText().trim()), "1");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: DESACTIVACIÓN LÓGICA (0)
        btnDesactivar.addActionListener(e -> {
            if (txtProCod.getText().trim().isEmpty()) return;
            cambiarEstado(Integer.parseInt(txtProCod.getText().trim()), "0");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: LIMPIAR FORMULARIO
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtProCod.setText(""); txtProPer.setText(""); 
            txtProViv.setText(""); txtProEscCod.setText(""); 
            txtProEscVig.setText(""); txtProFecCla.setText(""); 
            txtProPer.setEditable(true); 
            txtProViv.setEditable(true);
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private void cambiarEstado(int proCod, String estado) {
        String sql = "UPDATE public.h8m_propietario SET proestreg = ? WHERE procod = ?";
        try (Connection conn = conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setInt(2, proCod);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado del expediente de propiedad actualizado a: " + estado);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al alterar estado operacional:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Validador polimórfico adaptado a PostgreSQL (Soporta Integer y String)
    private boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) {
                stmt.setInt(1, (Integer) id);
            } else {
                stmt.setString(1, (String) id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean existeEscala(Connection conn, String escCod, int escVig) throws SQLException {
        String sql = "SELECT COUNT(*) FROM public.p9m_escala_tributo WHERE esccod = ? AND escvig = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escCod);
            stmt.setInt(2, escVig);
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
            throw new SQLException("Driver de comunicación PostgreSQL (org.postgresql.Driver) ausente.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}