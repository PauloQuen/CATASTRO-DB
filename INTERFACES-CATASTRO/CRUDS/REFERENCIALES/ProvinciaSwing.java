package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ProvinciaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtRegFK, txtNom;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public ProvinciaSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración con el Sistema) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO (Mapeado exacto con c1m_provincia en PostgreSQL)
        JPanel panelFormulario = new JPanel(new GridLayout(3, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Mantenimiento de Provincias (c1m_provincia) "));

        txtCod = new JTextField();   // procod (INT - PK)
        txtRegFK = new JTextField(); // proreg (INT - FK apuntando a c1m_region)
        txtNom = new JTextField();   // pronom (VARCHAR(60))

        panelFormulario.add(new JLabel("Código Provincia (procod):"));   panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Región FK (proreg):"));   panelFormulario.add(txtRegFK);
        panelFormulario.add(new JLabel("Nombre Provincia (pronom):"));  panelFormulario.add(txtNom);

        // 2. BOTONES DE CONTROL (Inclusión de Activación y Desactivación lógica)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar");

        // Estilos temáticos condicionales
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

        // 3. TABLA DE REGISTROS (Auditoría completa usando INNER JOIN para ver el nombre de la Región)
        String[] columnas = {"Código Prov.", "Código Región (FK)", "Nombre Provincia", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            String sqlSelect = "SELECT procod, proreg, pronom, proestreg FROM public.c1m_provincia ORDER BY procod DESC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("procod"));
                    fila.add(rs.getInt("proreg"));
                    fila.add(rs.getString("pronom"));
                    
                    String estReg = rs.getString("proestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar provincias: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso de selección de tabla a los campos de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtRegFK.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtCod.setEditable(false); // No se permite editar la PK en modificaciones
                } catch (Exception ex) {
                    // Prevenir inconsistencias visuales efímeras
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (Considera que 'procod' es un integer manual según tu DDL)
        btnInsertar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Provincia");
            int regionFK = validarEntero(txtRegFK.getText(), "Código Región FK");
            if (codigo == -1 || regionFK == -1) return;

            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la provincia es obligatorio.");
                return;
            }
            
            String sql = "INSERT INTO public.c1m_provincia (procod, proreg, pronom, proestreg) VALUES (?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, codigo);
                stmt.setInt(2, regionFK);
                stmt.setString(3, txtNom.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Provincia registrada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar (Asegúrese de que el código no esté duplicado y la Región FK exista):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Provincia");
            int regionFK = validarEntero(txtRegFK.getText(), "Código Región FK");
            if (codigo == -1 || regionFK == -1) return;
            
            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la provincia no puede estar vacío.");
                return;
            }
            
            String sql = "UPDATE public.c1m_provincia SET proreg = ?, pronom = ? WHERE procod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, regionFK);
                stmt.setString(2, txtNom.getText().trim());
                stmt.setInt(3, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Provincia modificada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> proestreg = '0')
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Provincia");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una provincia válida de la tabla para desactivar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta provincia a inactiva?\n(Se establecerá proestreg = '0')", 
                "Confirmación de Baja Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "La provincia ha sido inhabilitada (Desactivada) del listado activo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> proestreg = '1')
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Provincia");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una provincia inactiva para restaurarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ La provincia ha sido reactivada y dada de alta con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtRegFK.setText("");
            txtNom.setText(""); 
            txtCod.setEditable(true); // Se vuelve a habilitar para nuevos registros
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso centralizado para la mutación controlada del estado lógico (proestreg)
    private void cambiarEstadoRegistro(int proCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c1m_provincia SET proestreg = ? WHERE procod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, proCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó ningún registro bajo el código indicado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de mutación en la base de datos:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int validarEntero(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' es requerido.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' debe corresponder a un número entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
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
            throw new SQLException("Driver de PostgreSQL (org.postgresql.Driver) no hallado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}