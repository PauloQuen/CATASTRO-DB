package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DireccionSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtZon, txtViaTip, txtViaNom, txtNum, txtUrb, txtRef;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public DireccionSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración con SistemaCatastro) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

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

        // 2. BOTONES DE CONTROL - Modificados para persistencia de estados
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar");

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

        // 3. TABLA DE REGISTROS VISUALES (Incluye columna Estado Registro)
        String[] columnas = {"Código", "Zona (FK)", "Tipo Vía (FK)", "Vía Nombre", "Número", "Urbanización", "Referencia", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA (Muestra todos los estados ordenados descendentemente)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DirCod, DirZon, DirViaTip, DirViaNom, DirNum, DirUrb, DirRef, DirEstReg FROM c3m_direccion ORDER BY DirCod DESC")) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("DirCod"));
                    fila.add(rs.getInt("DirZon"));         
                    fila.add(rs.getString("DirViaTip"));   
                    fila.add(rs.getString("DirViaNom"));
                    fila.add(rs.getString("DirNum"));      
                    fila.add(rs.getString("DirUrb"));
                    fila.add(rs.getString("DirRef"));
                    
                    String estReg = rs.getString("DirEstReg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los registros: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
            
            String sql = "INSERT INTO c3m_direccion (DirCod, DirZon, DirViaTip, DirViaNom, DirNum, DirInt, DirUrb, DirRef, DirCodPos, DirLat, DirLon, DirEstReg) "
                       + "VALUES (?, ?, ?, ?, ?, '', ?, ?, '05001', -16.398, -71.536, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, codigo);
                stmt.setInt(2, zona);
                stmt.setString(3, txtViaTip.getText().trim());   
                stmt.setString(4, txtViaNom.getText().trim());
                stmt.setString(5, txtNum.getText().trim());      
                stmt.setString(6, txtUrb.getText().trim());
                stmt.setString(7, txtRef.getText().trim());
                stmt.executeUpdate();
                
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
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, zona);
                stmt.setString(2, txtViaTip.getText().trim());
                stmt.setString(3, txtViaNom.getText().trim());
                stmt.setString(4, txtNum.getText().trim());
                stmt.setString(5, txtUrb.getText().trim());
                stmt.setString(6, txtRef.getText().trim());
                stmt.setInt(7, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Dirección modificada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN ESTADO 0: DESACTIVAR REGISTRO (BAJA LÓGICA)
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Dirección");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Por favor, selecciona un registro válido de la tabla para desactivar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta dirección a inactiva?\n(Se establecerá DirEstReg = '0')", 
                "Confirmación de Inactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "La dirección ha sido inhabilitada (Desactivada) del sistema activo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // OPERACIÓN ESTADO 1: ACTIVAR REGISTRO (REACTIVACIÓN LÓGICA)
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Dirección");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una dirección inactiva desde el listado para restaurarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ La dirección ha sido reactivada y restaurada con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
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

    // Método encapsulado para la alteración atómica del Estado de Registro (DirEstReg)
    private void cambiarEstadoRegistro(int dirCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c3m_direccion SET DirEstReg = ? WHERE DirCod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, dirCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó el código de dirección en la base de datos.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de mutación en la base de datos:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

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
            throw new SQLException("Driver JDBC de PostgreSQL ausente.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}