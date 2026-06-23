import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PersonaSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db"
                            + "?useSSL=false&serverTimezone=America/Lima"
                            + "&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASSWORD = "pauloq3408"; 

    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private JTextField txtDni, txtNom, txtApePat, txtApeMat, txtIngreso, txtVivCod;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 

    public PersonaSwing() {
        setTitle("Módulo de Administración de Personas (H6M_PERSONA)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // 1. FORMULARIO (Cambiado a Datos de la Persona)
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de la Persona "));

        txtDni = new JTextField();       
        txtNom = new JTextField();       
        txtApePat = new JTextField();    
        txtApeMat = new JTextField();    
        txtIngreso = new JTextField();   
        txtVivCod = new JTextField();    

        panelFormulario.add(new JLabel("DNI (Exacto 8 dígitos):"));     panelFormulario.add(txtDni);
        panelFormulario.add(new JLabel("Nombres (PerNom):"));         panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Apellido Paterno:"));         panelFormulario.add(txtApePat);
        panelFormulario.add(new JLabel("Apellido Materno:"));         panelFormulario.add(txtApeMat);
        panelFormulario.add(new JLabel("Ingresos Mensuales S/:"));    panelFormulario.add(txtIngreso);
        panelFormulario.add(new JLabel("Código Vivienda (FK):"));     panelFormulario.add(txtVivCod);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN
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
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS VISUALES (Columna "Persona" en vez de Contribuyente)
        String[] columnas = {
            "DNI", "Persona", "Ingresos S/", "ID Casa", "Ubigeo Casa", "Vía Dirección", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        panelPrincipal.add(scrollTabla, BorderLayout.CENTER);

        // HILO DE LECTURA (READ)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT p.PerDNI, p.PerNom, p.PerApePat, p.PerApeMat, p.PerIng, p.PerViv, p.PerEstReg, "
                       + "       v.VivUbigeo, d.DirViaNom "
                       + "FROM H6M_PERSONA p "
                       + "INNER JOIN c3m_vivienda v ON p.PerViv = v.VivCod "
                       + "LEFT JOIN c3m_direccion d ON v.VivDir = d.DirCod "
                       + "ORDER BY p.PerEstReg DESC, p.PerApePat ASC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("PerDNI"));
                    
                    String nombreCompleto = rs.getString("PerApePat") + " " 
                                          + rs.getString("PerApeMat") + ", " 
                                          + rs.getString("PerNom");
                    fila.add(nombreCompleto);
                    fila.add(rs.getDouble("PerIng"));
                    fila.add(rs.getString("PerViv"));
                    fila.add(rs.getString("VivUbigeo"));
                    fila.add(getValorSeguro(rs.getString("DirViaNom")));
                    
                    // Muestra el carácter puro del estado alfanumérico
                    fila.add(rs.getString("PerEstReg") != null ? rs.getString("PerEstReg").trim() : "");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los registros: " + ex.getMessage());
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // TRASLADAR SELECCIÓN A CAJAS DE TEXTO
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                int dniSeleccionado = (int) modeloTabla.getValueAt(fila, 0);
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM H6M_PERSONA WHERE PerDNI = ?")) {
                    stmt.setInt(1, dniSeleccionado);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            txtDni.setText(String.format("%08d", rs.getInt("PerDNI")));
                            txtNom.setText(rs.getString("PerNom"));
                            txtApePat.setText(rs.getString("PerApePat"));
                            txtApeMat.setText(rs.getString("PerApeMat"));
                            txtIngreso.setText(String.valueOf(rs.getDouble("PerIng")));
                            txtVivCod.setText(rs.getString("PerViv"));
                            
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

        // ACCIÓN: REGISTRAR (CREATE)
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
                    JOptionPane.showMessageDialog(this, "El código de vivienda '" + viviendaCodigo + "' no existe en el sistema.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String sqlCheck = "SELECT PerEstReg FROM H6M_PERSONA WHERE PerDNI = ?";
                try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                    stmtCheck.setInt(1, dni);
                    try (ResultSet rsCheck = stmtCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            JOptionPane.showMessageDialog(this, "El DNI ingresado ya existe en la base de datos con estado '" + rsCheck.getString("PerEstReg").trim() + "'. Use los botones de estado para cambiarlo.", "Registro Duplicado", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }

                String sqlInsert = "INSERT INTO H6M_PERSONA (PerDNI, PerNom, PerApePat, PerApeMat, PerIng, PerViv, PerEstReg) VALUES (?, ?, ?, ?, ?, ?, '1')";
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

        // ACCIÓN: MODIFICAR (UPDATE)
        btnActualizar.addActionListener(e -> {
            if (camposEstanVacios()) return;
            
            String dniTexto = txtDni.getText().trim();
            int dni = Integer.parseInt(dniTexto);
            double ingreso = validarDouble(txtIngreso.getText(), "Ingresos Mensuales");
            if (ingreso == -1) return;
            
            String viviendaCodigo = txtVivCod.getText().trim();
            String sql = "UPDATE H6M_PERSONA SET PerNom=?, PerApePat=?, PerApeMat=?, PerIng=?, PerViv=? WHERE PerDNI=?";
            
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

        // ACCIÓN: BOTÓN ACTIVAR (1)
        btnActivar.addActionListener(e -> {
            if (txtDni.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una persona de la tabla para cambiar su estado a 1.");
                return;
            }
            int dni = Integer.parseInt(txtDni.getText().trim());
            
            String sql = "UPDATE H6M_PERSONA SET PerEstReg = '1' WHERE PerDNI = ?";
            try (Connection conn = conectar(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, dni);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Estado actualizado correctamente a: 1");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cambiar estado a '1':\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: BOTÓN DESACTIVAR (0)
        btnDesactivar.addActionListener(e -> {
            if (txtDni.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una persona de la tabla para cambiar su estado a 0.");
                return;
            }
            int dni = Integer.parseInt(txtDni.getText().trim());

            String sql = "UPDATE H6M_PERSONA SET PerEstReg = '0' WHERE PerDNI = ?";
            try (Connection conn = conectar(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, dni);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Estado actualizado correctamente a: 0");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cambiar estado a '0':\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtDni.setText(""); txtNom.setText(""); txtApePat.setText("");
            txtApeMat.setText(""); txtIngreso.setText(""); txtVivCod.setText("");
            txtDni.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private boolean validarFormatoDni(String dni) {
        if (!dni.matches("^\\d{8}$")) {
            JOptionPane.showMessageDialog(this, "El DNI debe contener exactamente 8 caracteres numéricos.", "Error de Validación", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean existeVivienda(Connection conn, String vivCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM c3m_vivienda WHERE VivCod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vivCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private double validarDouble(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El '" + nombreCampo + "' debe ser un valor decimal válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private boolean camposEstanVacios() {
        if (txtDni.getText().trim().isEmpty() || txtNom.getText().trim().isEmpty() || 
            txtApePat.getText().trim().isEmpty() || txtVivCod.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos DNI, Nombres, Apellido Paterno y Código de Vivienda son obligatorios.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver de MySQL no encontrado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PersonaSwing().setVisible(true);
        });
    }
}