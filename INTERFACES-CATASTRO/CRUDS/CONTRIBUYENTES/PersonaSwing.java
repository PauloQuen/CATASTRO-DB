package CRUDS.CONTRIBUYENTES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PersonaSwing extends JPanel {

    // Manteniendo tu base de datos tal cual está en tu pgAdmin
    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtDni, txtNom, txtApePat, txtApeMat, txtIngreso, txtVivCod;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    private boolean isAjustando = false; 

    private JPanel contenedorPadre;
    private String destinoRetorno;

    public PersonaSwing(JPanel contenedorPadre, String destinoRetorno) {
        this.contenedorPadre = contenedorPadre;
        this.destinoRetorno = destinoRetorno;

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

        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de la Persona (H6M_PERSONA) "));

        txtDni = new JTextField();       
        txtNom = new JTextField();       
        txtApePat = new JTextField();    
        txtApeMat = new JTextField();    
        txtIngreso = new JTextField();   
        txtVivCod = new JTextField();    

        panelFormulario.add(new JLabel("DNI (Exacto 8 dígitos):"));   panelFormulario.add(txtDni);
        panelFormulario.add(new JLabel("Nombres:"));                  panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Apellido Paterno:"));         panelFormulario.add(txtApePat);
        panelFormulario.add(new JLabel("Apellido Materno:"));         panelFormulario.add(txtApeMat);
        panelFormulario.add(new JLabel("Ingresos Mensuales S/:"));    panelFormulario.add(txtIngreso);
        panelFormulario.add(new JLabel("Código Vivienda (FK):"));     panelFormulario.add(txtVivCod);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
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

        String[] columnas = {
            "DNI", "Persona", "Ingresos S/", "ID Casa", "Ubigeo Casa", "Vía Dirección", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT p.perdni, p.pernom, p.perapepat, p.perapemat, p.pering, p.perviv, p.perestreg, "
                       + "       v.vivubigeo, d.dirvianom "
                       + "FROM public.h6m_persona p "
                       + "INNER JOIN public.c3m_vivienda v ON p.perviv = v.vivcod "
                       + "LEFT JOIN public.c3m_direccion d ON v.vivdir = d.dircod "
                       + "ORDER BY p.perestreg DESC, p.perapepat ASC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("perdni"));
                    
                    String nombreCompleto = rs.getString("perapepat") + " " 
                                          + rs.getString("perapemat") + ", " 
                                          + rs.getString("pernom");
                    fila.add(nombreCompleto);
                    fila.add(rs.getDouble("pering"));
                    fila.add(rs.getString("perviv"));
                    fila.add(rs.getString("vivubigeo"));
                    fila.add(getValorSeguro(rs.getString("dirvianom")));
                    fila.add(rs.getString("perestreg") != null ? rs.getString("perestreg").trim() : "1");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los registros: " + ex.getMessage());
            } finally {
                isAjustando = false; 
            }
        };

        Timer timer = new Timer(200, e -> cargarDatos.run());
        timer.setRepeats(false);
        timer.start();

        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                int dniSeleccionado = (int) modeloTabla.getValueAt(fila, 0);
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM public.h6m_persona WHERE perdni = ?")) {
                    stmt.setInt(1, dniSeleccionado);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            txtDni.setText(String.format("%08d", rs.getInt("perdni")));
                            txtNom.setText(rs.getString("pernom"));
                            txtApePat.setText(rs.getString("perapepat"));
                            txtApeMat.setText(rs.getString("perapemat"));
                            txtIngreso.setText(String.valueOf(rs.getDouble("pering")));
                            txtVivCod.setText(rs.getString("perviv"));
                            txtDni.setEditable(false); 
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Error al recuperar selección: " + ex.getMessage());
                } finally {
                    isAjustando = false;
                }
            }
        });

        btnInsertar.addActionListener(e -> {
            if (camposEstanVacios()) return;
            
            String dniTexto = txtDni.getText().trim();
            if (!validarFormatoDni(dniTexto)) return; 
            
            int dni = Integer.parseInt(dniTexto);
            double ingreso = validarDouble(txtIngreso.getText(), "Ingresos Mensuales");
            if (ingreso == -1) return; 
            
            String viviendaCodigo = txtVivCod.getText().trim();
            
            try (Connection conn = conectar()) {
                if (!existeVivienda(conn, viviendaCodigo)) {
                    JOptionPane.showMessageDialog(this, "El código de vivienda '" + viviendaCodigo + "' no existe.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String sqlCheck = "SELECT perestreg FROM public.h6m_persona WHERE perdni = ?";
                try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                    stmtCheck.setInt(1, dni);
                    try (ResultSet rsCheck = stmtCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            JOptionPane.showMessageDialog(this, "El DNI ingresado ya existe (Estado: " + rsCheck.getString("perestreg").trim() + ").", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }

                String sqlInsert = "INSERT INTO public.h6m_persona (perdni, pernom, perapepat, perapemat, pering, perviv, perestreg) VALUES (?, ?, ?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, dni);
                    stmt.setString(2, txtNom.getText().trim());
                    stmt.setString(3, txtApePat.getText().trim());
                    stmt.setString(4, txtApeMat.getText().trim());
                    stmt.setDouble(5, ingreso);
                    stmt.setString(6, viviendaCodigo);
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Persona registrada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnActualizar.addActionListener(e -> {
            if (camposEstanVacios()) return;
            
            int dni = Integer.parseInt(txtDni.getText().trim());
            double ingreso = validarDouble(txtIngreso.getText(), "Ingresos Mensuales");
            if (ingreso == -1) return;
            
            String viviendaCodigo = txtVivCod.getText().trim();
            String sql = "UPDATE public.h6m_persona SET pernom=?, perapepat=?, perapemat=?, pering=?, perviv=? WHERE perdni=?";
            
            try (Connection conn = conectar()) {
                if (!existeVivienda(conn, viviendaCodigo)) {
                    JOptionPane.showMessageDialog(this, "El código de vivienda '" + viviendaCodigo + "' no existe.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, txtNom.getText().trim());
                    stmt.setString(2, txtApePat.getText().trim());
                    stmt.setString(3, txtApeMat.getText().trim());
                    stmt.setDouble(4, ingreso);
                    stmt.setString(5, viviendaCodigo);
                    stmt.setInt(6, dni);
                    int filas = stmt.executeUpdate();
                    if (filas > 0) {
                        JOptionPane.showMessageDialog(this, "¡Datos modificados correctamente!");
                    }
                }
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnActivar.addActionListener(e -> {
            if (txtDni.getText().trim().isEmpty()) return;
            int dni = Integer.parseInt(txtDni.getText().trim());
            cambiarEstado(dni, "1", cargarDatos);
        });

        btnDesactivar.addActionListener(e -> {
            if (txtDni.getText().trim().isEmpty()) return;
            int dni = Integer.parseInt(txtDni.getText().trim());
            cambiarEstado(dni, "0", cargarDatos);
        });

        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtDni.setText(""); txtNom.setText(""); txtApePat.setText("");
            txtApeMat.setText(""); txtIngreso.setText(""); txtVivCod.setText("");
            txtDni.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private void cambiarEstado(int dni, String estado, Runnable postAccion) {
        String sql = "UPDATE public.h6m_persona SET perestreg = ? WHERE perdni = ?";
        try (Connection conn = conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setInt(2, dni);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado actualizado correctamente a: " + estado);
            postAccion.run();
            txtDni.setEditable(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar estado:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validarFormatoDni(String dni) {
        return dni.matches("^\\d{8}$");
    }

    private boolean existeVivienda(Connection conn, String vivCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM public.c3m_vivienda WHERE vivcod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vivCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private double validarDouble(String texto, String nombreCampo) {
        try { return Double.parseDouble(texto.trim()); } 
        catch (Exception e) { return -1; }
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private boolean camposEstanVacios() {
        return txtDni.getText().trim().isEmpty() || txtNom.getText().trim().isEmpty() || txtVivCod.getText().trim().isEmpty();
    }

    private Connection conectar() throws SQLException {
        try { Class.forName(DRIVER); } 
        catch (ClassNotFoundException e) { throw new SQLException("Driver Postgres no encontrado."); }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}