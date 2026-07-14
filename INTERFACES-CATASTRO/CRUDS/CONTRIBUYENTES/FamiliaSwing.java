package CRUDS.CONTRIBUYENTES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class FamiliaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtFamCod, txtFamCab, txtFamNom, txtFamViv, txtFamNumMiem;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 
    private Container principalContainer;
    private String vistaOrigen;

    public FamiliaSwing(Container container, String vistaOrigen) {
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
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos del Grupo Familiar "));

        txtFamCod = new JTextField();
        txtFamCod.setEditable(false); 
        txtFamCod.setBackground(new Color(240, 240, 240));
        
        txtFamCab = new JTextField();      
        txtFamNom = new JTextField();      
        txtFamViv = new JTextField();      
        txtFamNumMiem = new JTextField();    

        panelFormulario.add(new JLabel("Código Familia (Auto):"));   panelFormulario.add(txtFamCod);
        panelFormulario.add(new JLabel("DNI Jefe Hogar (INT FK):")); panelFormulario.add(txtFamCab);
        panelFormulario.add(new JLabel("Denominación Familiar:"));   panelFormulario.add(txtFamNom);
        panelFormulario.add(new JLabel("Código Vivienda (FK):"));    panelFormulario.add(txtFamViv);
        panelFormulario.add(new JLabel("Nº de Miembros:"));          panelFormulario.add(txtFamNumMiem);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE ACCIÓN (Inferiores)
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

        // Estructura del Panel Norte (Navegación arriba, Formulario al centro, Botones abajo)
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(panelNavegacion, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS VISUALES
        String[] columnas = {
            "Código Fam", "DNI Jefe", "Jefe (Nombres)", "Denominación Familiar", "Cod Vivienda", "Nº Miembros", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // HILO DE LECTURA (READ - PostgreSQL)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT f.FamCod, f.FamCab, p.PerNom, p.PerApePat, p.PerApeMat, f.FamNom, f.FamViv, f.FamNumMiem, f.FamEstReg "
                       + "FROM h7m_familia f "
                       + "INNER JOIN h6m_persona p ON f.FamCab = p.PerDNI "
                       + "ORDER BY f.FamEstReg DESC, f.FamCod DESC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("FamCod"));
                    fila.add(String.format("%08d", rs.getInt("FamCab"))); 
                    
                    String nombreJefe = rs.getString("PerApePat").trim() + " " 
                                      + rs.getString("PerApeMat").trim() + ", " 
                                      + rs.getString("PerNom").trim();
                    fila.add(nombreJefe);
                    fila.add(rs.getString("FamNom"));
                    fila.add(getValorSeguro(rs.getString("FamViv")));
                    fila.add(rs.getInt("FamNumMiem"));
                    fila.add(rs.getString("FamEstReg") != null ? rs.getString("FamEstReg").trim() : "");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los registros de familias:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        // Inicialización diferida para evitar parpadeos
        Timer timer = new Timer(200, e -> cargarDatos.run());
        timer.setRepeats(false);
        timer.start();

        // TRASLADAR SELECCIÓN A CAJAS DE TEXTO
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                int codigoSeleccionado = (int) modeloTabla.getValueAt(fila, 0);
                
                try (Connection conn = conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM h7m_familia WHERE FamCod = ?")) {
                    stmt.setInt(1, codigoSeleccionado);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            txtFamCod.setText(String.valueOf(rs.getInt("FamCod")));
                            txtFamCab.setText(String.format("%08d", rs.getInt("FamCab")));
                            txtFamNom.setText(rs.getString("FamNom") != null ? rs.getString("FamNom").trim() : "");
                            txtFamViv.setText(rs.getString("FamViv") != null ? rs.getString("FamViv").trim() : "");
                            txtFamNumMiem.setText(String.valueOf(rs.getInt("FamNumMiem")));
                            
                            txtFamCab.setEditable(false); 
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Error al recuperar selección: " + ex.getMessage());
                } finally {
                    isAjustando = false;
                }
            }
        });

        // ACCIÓN: REGISTRAR (CREATE - PostgreSQL 1:1)
        btnInsertar.addActionListener(e -> {
            if (camposEstanVacios()) return;
            
            String cabDniTexto = txtFamCab.getText().trim();
            if (!cabDniTexto.matches("^\\d{1,8}$")) {
                JOptionPane.showMessageDialog(this, "El DNI debe ser un número válido de hasta 8 dígitos.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int famCab = Integer.parseInt(cabDniTexto);
            String famNom = txtFamNom.getText().trim();
            String famViv = txtFamViv.getText().trim();
            if (famViv.isEmpty()) famViv = null; 
            
            int numMiem;
            try {
                numMiem = Integer.parseInt(txtFamNumMiem.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El número de miembros debe ser un entero válido.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try (Connection conn = conectar()) {
                if (!existeId(conn, "h6m_persona", "perdni", famCab)) {
                    JOptionPane.showMessageDialog(this, "El DNI del jefe de familia no existe en la tabla h6m_persona.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (existeId(conn, "h7m_familia", "famcab", famCab)) {
                    JOptionPane.showMessageDialog(this, 
                        "Error de Validación:\nEl ciudadano con DNI " + cabDniTexto + " ya se encuentra registrado como cabeza de familia.", 
                        "Restricción Unicidad Jefe (1:1)", 
                        JOptionPane.ERROR_MESSAGE);
                    return; 
                }
                
                if (famViv != null && !existeId(conn, "c3m_vivienda", "vivcod", famViv)) {
                    JOptionPane.showMessageDialog(this, "El código de vivienda '" + famViv + "' no existe.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sqlInsert = "INSERT INTO h7m_familia (FamCab, FamNom, FamViv, FamNumMiem, FamEstReg) VALUES (?, ?, ?, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, famCab);
                    stmt.setString(2, famNom);
                    stmt.setString(3, famViv); 
                    stmt.setInt(4, numMiem);
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Grupo Familiar registrado con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar familia:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: MODIFICAR (UPDATE)
        btnActualizar.addActionListener(e -> {
            if (txtFamCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro de la tabla para modificar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (camposEstanVacios()) return;
            
            int codigo = Integer.parseInt(txtFamCod.getText().trim());
            String famNom = txtFamNom.getText().trim();
            String famViv = txtFamViv.getText().trim();
            if (famViv.isEmpty()) famViv = null;
            
            int numMiem;
            try {
                numMiem = Integer.parseInt(txtFamNumMiem.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El número de miembros debe ser un entero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try (Connection conn = conectar()) {
                if (famViv != null && !existeId(conn, "c3m_vivienda", "vivcod", famViv)) {
                    JOptionPane.showMessageDialog(this, "La vivienda '" + famViv + "' no existe.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sqlUpdate = "UPDATE h7m_familia SET FamNom=?, FamViv=?, FamNumMiem=? WHERE FamCod=?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setString(1, famNom);
                    stmt.setString(2, famViv);
                    stmt.setInt(3, numMiem);
                    stmt.setInt(4, codigo);
                    stmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "¡Registro familiar actualizado!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: BOTÓN ACTIVAR (1)
        btnActivar.addActionListener(e -> {
            if (txtFamCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una fila de la tabla.");
                return;
            }
            int codigo = Integer.parseInt(txtFamCod.getText().trim());
            cambiarEstado(codigo, "1");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: BOTÓN DESACTIVAR (0)
        btnDesactivar.addActionListener(e -> {
            if (txtFamCod.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una fila de la tabla.");
                return;
            }
            int codigo = Integer.parseInt(txtFamCod.getText().trim());
            cambiarEstado(codigo, "0");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // ACCIÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtFamCod.setText(""); txtFamCab.setText(""); 
            txtFamNom.setText(""); txtFamViv.setText(""); txtFamNumMiem.setText(""); 
            txtFamCab.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });

        // ACCIÓN: VOLVER AL MENÚ PRINCIPAL
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

    private void cambiarEstado(int codigo, String estado) {
        String sql = "UPDATE h7m_familia SET FamEstReg = ? WHERE FamCod = ?";
        try (Connection conn = conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setInt(2, codigo);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado cambiado correctamente a: " + estado);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar estado:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql;
        if (id instanceof Integer) {
            sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        } else {
            sql = "SELECT COUNT(*) FROM " + tabla + " WHERE LOWER(" + campoId + "::text) = LOWER(?)";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) {
                stmt.setInt(1, (Integer) id);
            } else {
                stmt.setString(1, id.toString().trim());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "Sin asignar" : obj.toString().trim();
    }

    private boolean camposEstanVacios() {
        if (txtFamCab.getText().trim().isEmpty() || txtFamNom.getText().trim().isEmpty() || 
            txtFamNumMiem.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos DNI Jefe, Denominación y Número de Miembros son obligatorios.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
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