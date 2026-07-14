package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DistritoSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtProFK, txtNom;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public DistritoSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración con el Panel Principal) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO (Mapeado exacto con c1m_distrito de PostgreSQL)
        JPanel panelFormulario = new JPanel(new GridLayout(3, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Mantenimiento de Distritos (c1m_distrito) "));

        txtCod = new JTextField();   // discod (INT - PK manual)
        txtProFK = new JTextField(); // dispro (INT - FK apuntando a c1m_provincia)
        txtNom = new JTextField();   // disnom (VARCHAR(60))

        panelFormulario.add(new JLabel("Código Distrito (discod):"));   panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Provincia FK (dispro):")); panelFormulario.add(txtProFK);
        panelFormulario.add(new JLabel("Nombre Distrito (disnom):"));   panelFormulario.add(txtNom);

        // 2. BOTONES DE CONTROL (Inclusión de Activación y Desactivación lógica)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar");

        // Estilos temáticos condicionales para auditoría de estados
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

        // 3. TABLA DE REGISTROS (Muestra el estado explícito de auditoría)
        String[] columnas = {"Código Dist.", "Código Prov. (FK)", "Nombre Distrito", "Estado Reg."};
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
            String sqlSelect = "SELECT discod, dispro, disnom, disestreg FROM public.c1m_distrito ORDER BY discod DESC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("discod"));
                    fila.add(rs.getInt("dispro"));
                    fila.add(rs.getString("disnom"));
                    
                    String estReg = rs.getString("disestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar distritos: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
                    txtProFK.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtCod.setEditable(false); // No modificable en transacciones UPDATE
                } catch (Exception ex) {
                    // Prevenir anomalías en la sincronización visual
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR
        btnInsertar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Distrito");
            int provinciaFK = validarEntero(txtProFK.getText(), "Código Provincia FK");
            if (codigo == -1 || provinciaFK == -1) return;

            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre del distrito es obligatorio.");
                return;
            }
            
            String sql = "INSERT INTO public.c1m_distrito (discod, dispro, disnom, disestreg) VALUES (?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, codigo);
                stmt.setInt(2, provinciaFK);
                stmt.setString(3, txtNom.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Distrito registrado exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar (Verifique PK duplicada o si la Provincia FK existe):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Distrito");
            int provinciaFK = validarEntero(txtProFK.getText(), "Código Provincia FK");
            if (codigo == -1 || provinciaFK == -1) return;
            
            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre del distrito no puede ser un valor vacío.");
                return;
            }
            
            String sql = "UPDATE public.c1m_distrito SET dispro = ?, disnom = ? WHERE discod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, provinciaFK);
                stmt.setString(2, txtNom.getText().trim());
                stmt.setInt(3, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Distrito modificado correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> disestreg = '0')
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Distrito");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione un distrito del listado para darlo de baja.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de este distrito a inactivo?\n(Se establecerá disestreg = '0')", 
                "Confirmación de Baja Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "El distrito ha sido inhabilitado (Desactivado) del catálogo operativo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> disestreg = '1')
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Distrito");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un registro inactivo desde la tabla para restaurarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ El distrito seleccionado ha sido reactivado y restaurado con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtProFK.setText("");
            txtNom.setText(""); 
            txtCod.setEditable(true); // Se rehabilita la PK para inserciones nuevas
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso centralizado para la manipulación controlada del estado de registro (disestreg)
    private void cambiarEstadoRegistro(int disCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c1m_distrito SET disestreg = ? WHERE discod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, disCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se localizó ningún registro bajo el código provisto.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' requiere un valor entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
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
            throw new SQLException("Driver JDBC de PostgreSQL ausente.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}