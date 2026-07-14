package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class MunicipalidadSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtDisFK, txtNom, txtAlc, txtCor, txtTel;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public MunicipalidadSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO (Mapeo completo del DDL de c1m_municipalidad)
        JPanel panelFormulario = new JPanel(new GridLayout(6, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Mantenimiento de Municipalidades (c1m_municipalidad) "));

        txtCod = new JTextField(); txtCod.setEditable(false); // serial (Autoincrementable)
        txtDisFK = new JTextField(); // mundis (FK)
        txtNom = new JTextField();   // munnom
        txtAlc = new JTextField();   // munalcnom
        txtCor = new JTextField();   // muncor (Correo)
        txtTel = new JTextField();   // muntelcon (Teléfono)

        panelFormulario.add(new JLabel("Código Municipalidad (muncod):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Distrito FK (mundis):"));    panelFormulario.add(txtDisFK);
        panelFormulario.add(new JLabel("Nombre Municipalidad (munnom):")); panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Nombre Alcalde (munalcnom):"));     panelFormulario.add(txtAlc);
        panelFormulario.add(new JLabel("Correo Electrónico (muncor):"));   panelFormulario.add(txtCor);
        panelFormulario.add(new JLabel("Teléfono Contacto (muntelcon):")); panelFormulario.add(txtTel);

        // 2. BOTONES DE CONTROL (Inclusión de Activación y Desactivación lógica)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar");
        JButton btnActualizar = new JButton("Modificar");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar");

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

        // 3. TABLA DE REGISTROS
        String[] columnas = {"Muncod", "Mundis (FK)", "Municipalidad", "Alcalde", "Correo", "Teléfono", "Estado Reg."};
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
            String sqlSelect = "SELECT muncod, mundis, munnom, munalcnom, muncor, muntelcon, munestreg FROM public.c1m_municipalidad ORDER BY muncod DESC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("muncod"));
                    fila.add(rs.getInt("mundis"));
                    fila.add(rs.getString("munnom"));
                    fila.add(rs.getString("munalcnom"));
                    fila.add(rs.getString("muncor"));
                    fila.add(rs.getString("muntelcon"));
                    
                    String estReg = rs.getString("munestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar municipalidades: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso de selección de la tabla a las cajas de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtDisFK.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtAlc.setText(getValorSeguro(modeloTabla.getValueAt(fila, 3)));
                    txtCor.setText(getValorSeguro(modeloTabla.getValueAt(fila, 4)));
                    txtTel.setText(getValorSeguro(modeloTabla.getValueAt(fila, 5)));
                } catch (Exception ex) {
                    // Evitar interrupciones visuales
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (muncod omitido por ser serial autonumérico)
        btnInsertar.addActionListener(e -> {
            int distritoFK = validarEntero(txtDisFK.getText(), "Código Distrito FK");
            if (distritoFK == -1) return;

            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la municipalidad es un campo obligatorio.");
                return;
            }
            
            String sql = "INSERT INTO public.c1m_municipalidad (mundis, munnom, munalcnom, muncor, muntelcon, munestreg) VALUES (?, ?, ?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, distritoFK);
                stmt.setString(2, txtNom.getText().trim());
                stmt.setString(3, txtAlc.getText().trim().isEmpty() ? null : txtAlc.getText().trim());
                stmt.setString(4, txtCor.getText().trim().isEmpty() ? null : txtCor.getText().trim());
                stmt.setString(5, txtTel.getText().trim().isEmpty() ? null : txtTel.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Municipalidad registrada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar (Verifique si el Distrito FK existe):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Municipalidad");
            int distritoFK = validarEntero(txtDisFK.getText(), "Código Distrito FK");
            if (codigo == -1 || distritoFK == -1) return;
            
            if (txtNom.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la municipalidad no puede estar vacío.");
                return;
            }
            
            String sql = "UPDATE public.c1m_municipalidad SET mundis = ?, munnom = ?, munalcnom = ?, muncor = ?, muntelcon = ? WHERE muncod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, distritoFK);
                stmt.setString(2, txtNom.getText().trim());
                stmt.setString(3, txtAlc.getText().trim().isEmpty() ? null : txtAlc.getText().trim());
                stmt.setString(4, txtCor.getText().trim().isEmpty() ? null : txtCor.getText().trim());
                stmt.setString(5, txtTel.getText().trim().isEmpty() ? null : txtTel.getText().trim());
                stmt.setInt(6, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "¡Municipalidad modificada correctamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> munestreg = '0')
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Municipalidad");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro válido de la tabla para desactivar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta municipalidad a inactiva?\n(Se establecerá munestreg = '0')", 
                "Confirmación de Baja Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "La municipalidad ha sido dada de baja lógicamente.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> munestreg = '1')
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Municipalidad");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un registro inactivo de la tabla para restaurarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ La municipalidad seleccionada ha sido reactivada con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtDisFK.setText("");
            txtNom.setText(""); 
            txtAlc.setText("");
            txtCor.setText("");
            txtTel.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    private void cambiarEstadoRegistro(int munCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c1m_municipalidad SET munestreg = ? WHERE muncod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, munCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el registro indicado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar estado en la base de datos:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' debe ser un valor entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
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
            throw new SQLException("Driver de PostgreSQL no hallado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}