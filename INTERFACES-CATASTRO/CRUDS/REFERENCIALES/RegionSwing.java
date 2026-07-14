package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class RegionSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtNom;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public RegionSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración con el Panel de Selección del Sistema) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO (Mapeado exacto con c1m_region en minúsculas/case-insensitive estándar de PG)
        JPanel panelFormulario = new JPanel(new GridLayout(2, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Mantenimiento de Regiones (c1m_region) "));

        txtCod = new JTextField();
        txtCod.setEditable(false); // Llave autogenerada (serial)
        txtNom = new JTextField();

        panelFormulario.add(new JLabel("Código Región (regcod):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Nombre Región (regnom):")); panelFormulario.add(txtNom);

        // 2. BOTONES DE CONTROL (Inclusión de Activar/Desactivar lógicos)
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

        // 3. TABLA DE REGISTROS VISUALES (Incluye auditoría de estado regestreg)
        String[] columnas = {"Código", "Nombre Región", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA (Listado de auditoría completa orden descendente)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT regcod, regnom, regestreg FROM c1m_region ORDER BY regcod DESC")) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("regcod"));
                    fila.add(rs.getString("regnom"));
                    
                    String estReg = rs.getString("regestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar las regiones: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso seguro de selección desde la tabla a las cajas de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                } catch (Exception ex) {
                    // Prevenir fallos visuales menores
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (No envía código ya que es serial autonumérico)
        btnInsertar.addActionListener(e -> {
            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la región es un campo obligatorio.");
                return;
            }
            
            String sql = "INSERT INTO c1m_region (regnom, regestreg) VALUES (?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, txtNom.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Región registrada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Región");
            if (codigo == -1) return;
            
            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la región es obligatorio para ser modificado.");
                return;
            }
            
            String sql = "UPDATE c1m_region SET regnom = ? WHERE regcod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, txtNom.getText().trim());
                stmt.setInt(2, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Región modificada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> regestreg = '0')
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Región");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una región del listado para desactivarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta región a inactiva?\n(Se establecerá regestreg = '0')", 
                "Confirmación de Baja Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "La región ha sido inhabilitada (Desactivada) con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> regestreg = '1')
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Región");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro inactivo desde la tabla para restaurarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "La región ha sido reactivada y habilitada en el sistema.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtNom.setText(""); 
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso centralizado y atómico para la mutación del estado lógico (regestreg)
    private void cambiarEstadoRegistro(int regCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE c1m_region SET regestreg = ? WHERE regcod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, regCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el registro con el código especificado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de mutación en la base de datos:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int validarEntero(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione primero un registro válido de la tabla.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El valor de '" + nombreCampo + "' no es un identificador numérico válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de PostgreSQL ausente en el entorno de ejecución.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}