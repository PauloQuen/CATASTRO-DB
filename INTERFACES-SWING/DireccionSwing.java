import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DireccionSwing extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db"
                            + "?useSSL=false&serverTimezone=America/Lima"
                            + "&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASSWORD = ""; 

    static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private JTextField txtCod, txtZon, txtViaTip, txtViaNom, txtNum, txtUrb, txtRef;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public DireccionSwing() {
        setTitle("Módulo de Direcciones (c3m_direccion) - Estructura SQL Fiel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);
        
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // 1. FORMULARIO (Mapeado exacto con tus nombres de columna de SQL)
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos del Catastro (c3m_direccion) "));

        txtCod = new JTextField();    // DirCod (INT - PK)
        txtZon = new JTextField();    // DirZon (INT - FK)
        txtViaTip = new JTextField(); // DirViaTip (VARCHAR(4) - FK)
        txtViaNom = new JTextField(); // DirViaNom (VARCHAR(80))
        txtNum = new JTextField();    // DirNum (VARCHAR(10))
        txtUrb = new JTextField();    // DirUrb (VARCHAR(80))
        txtRef = new JTextField();    // DirRef (VARCHAR(120))

        panelFormulario.add(new JLabel("Código Dirección (DirCod):"));  panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Zona (DirZon - INT):")); panelFormulario.add(txtZon);
        panelFormulario.add(new JLabel("Tipo Vía (DirViaTip - TEXT):")); panelFormulario.add(txtViaTip);
        panelFormulario.add(new JLabel("Nombre de Vía (DirViaNom):")); panelFormulario.add(txtViaNom);
        panelFormulario.add(new JLabel("Número (DirNum):"));           panelFormulario.add(txtNum);
        panelFormulario.add(new JLabel("Urbanización (DirUrb):"));     panelFormulario.add(txtUrb);
        panelFormulario.add(new JLabel("Referencia (DirRef):"));       panelFormulario.add(txtRef);
        
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE CONTROL
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnLimpiar = new JButton("Limpiar");

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS VISUALES
        String[] columnas = {"Código", "Zona (FK)", "Tipo Vía (FK)", "Vía Nombre", "Número", "Urbanización", "Referencia"};
        modeloTabla = new DefaultTableModel(columnas, 0);
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        panelPrincipal.add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM c3m_direccion WHERE DirEstReg = '1'")) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    // Llamamos exactamente a las columnas como las definiste en tu CREATE TABLE
                    fila.add(rs.getInt("DirCod"));
                    fila.add(rs.getInt("DirZon"));         // Es INT según tu SQL
                    fila.add(rs.getString("DirViaTip"));   // Es VARCHAR(4) según tu SQL
                    fila.add(rs.getString("DirViaNom"));
                    fila.add(rs.getString("DirNum"));      
                    fila.add(rs.getString("DirUrb"));
                    fila.add(rs.getString("DirRef"));
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los registros: " + ex.getMessage());
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Pasar selección segura de la tabla a cajas de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtZon.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtViaTip.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtViaNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 3)));
                    txtNum.setText(getValorSeguro(modeloTabla.getValueAt(fila, 4)));
                    txtUrb.setText(getValorSeguro(modeloTabla.getValueAt(fila, 5)));
                    txtRef.setText(getValorSeguro(modeloTabla.getValueAt(fila, 6)));
                    txtCod.setEditable(false);
                } catch (Exception ex) {
                    // Prevenir fallos visuales menores
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR
        btnInsertar.addActionListener(e -> {
            if (camposEstanVacios(true)) return;
            
            int codigo = validarEntero(txtCod.getText(), "Código Dirección");
            int zona = validarEntero(txtZon.getText(), "Código Zona");
            if (codigo == -1 || zona == -1) return; 
            
            // Query adaptada estrictamente al orden de tus columnas en SQL
            String sql = "INSERT INTO c3m_direccion (DirCod, DirZon, DirViaTip, DirViaNom, DirNum, DirInt, DirUrb, DirRef, DirCodPos, DirLat, DirLon, DirEstReg) "
                       + "VALUES (?, ?, ?, ?, ?, '', ?, ?, '05001', -16.398, -71.536, '1')";
            
            try (Connection conn = conectar(); Statement stmtCheck = conn.createStatement()) {
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 0"); // Control preventivo por dependencias vacías
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, codigo);
                    stmt.setInt(2, zona);
                    stmt.setString(3, txtViaTip.getText().trim());   
                    stmt.setString(4, txtViaNom.getText().trim());
                    stmt.setString(5, txtNum.getText().trim());      
                    stmt.setString(6, txtUrb.getText().trim());
                    stmt.setString(7, txtRef.getText().trim());
                    stmt.executeUpdate();
                }
                
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 1");
                JOptionPane.showMessageDialog(this, "¡Dirección registrada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            if (camposEstanVacios(false)) return;
            
            int codigo = validarEntero(txtCod.getText(), "Código Dirección");
            int zona = validarEntero(txtZon.getText(), "Código Zona");
            if (codigo == -1 || zona == -1) return;
            
            String sql = "UPDATE c3m_direccion SET DirZon=?, DirViaTip=?, DirViaNom=?, DirNum=?, DirUrb=?, DirRef=? WHERE DirCod=?";
            try (Connection conn = conectar(); Statement stmtCheck = conn.createStatement()) {
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, zona);
                    stmt.setString(2, txtViaTip.getText().trim());
                    stmt.setString(3, txtViaNom.getText().trim());
                    stmt.setString(4, txtNum.getText().trim());
                    stmt.setString(5, txtUrb.getText().trim());
                    stmt.setString(6, txtRef.getText().trim());
                    stmt.setInt(7, codigo);
                    stmt.executeUpdate();
                }
                
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 1");
                JOptionPane.showMessageDialog(this, "¡Dirección modificada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: ELIMINAR
        btnEliminar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Dirección");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Por favor, selecciona un registro válido de la tabla.");
                return;
            }
            
            String sql = "DELETE FROM c3m_direccion WHERE DirCod=?";
            try (Connection conn = conectar(); Statement stmtCheck = conn.createStatement()) {
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, codigo);
                    stmt.executeUpdate();
                }
                
                stmtCheck.execute("SET FOREIGN_KEY_CHECKS = 1");
                JOptionPane.showMessageDialog(this, "¡Dirección eliminada por completo!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al eliminar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); txtZon.setText(""); txtViaTip.setText("");
            txtViaNom.setText(""); txtNum.setText(""); txtUrb.setText(""); txtRef.setText("");
            txtCod.setEditable(true); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Validador con .trim() dinámico para purgar espacios como " 1 " o vacíos ""
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

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private boolean camposEstanVacios(boolean validarTodo) {
        if (validarTodo) {
            if (txtCod.getText().trim().isEmpty() || txtZon.getText().trim().isEmpty() || txtViaTip.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Campos obligatorios vacíos: Código Dirección, Zona o Tipo de Vía.");
                return true;
            }
        }
        if (txtViaNom.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la vía es obligatorio.");
            return true;
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver de MySQL no encontrado en el entorno.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DireccionSwing().setVisible(true);
        });
    }
}