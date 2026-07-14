package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TipoPredioSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtNom, txtDes;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public TipoPredioSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Navegación CardLayout) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO DE CAPTURA (Sincronizado con c5m_tipo_predio de PostgreSQL)
        JPanel panelFormulario = new JPanel(new GridLayout(3, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Clasificación y Tipos de Predio (c5m_tipo_predio) "));

        txtCod = new JTextField();   // tipprecod (VARCHAR(4) - PK Manual)
        txtNom = new JTextField();   // tipprenom (VARCHAR(60))
        txtDes = new JTextField();   // tippredes (VARCHAR(200))

        panelFormulario.add(new JLabel("Código Tipo Predio (tipprecod - Máx 4 caracteres):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Nombre / Clasificación (tipprenom):"));               panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Descripción Adicional (tippredes):"));               panelFormulario.add(txtDes);

        // 2. BOTONES DE ACCIÓN ADMINISTRATIVA (Gestión de estados lógicos)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Tipo");
        JButton btnActualizar = new JButton("Modificar Tipo");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Paleta semántica para estados operativos
        btnDesactivar.setBackground(new Color(255, 195, 195));
        btnActivar.setBackground(new Color(195, 230, 195));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnActivar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        panelSuperior.add(pnlSuperiorWrapper, BorderLayout.NORTH);
        panelSuperior.add(panelFormulario, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        // 3. TABLA DE DATOS INTEGRAL
        String[] columnas = {"Código Tipo (PK)", "Clasificación de Predio", "Descripción", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // LÓGICA DE CARGA DESDE POSTGRESQL
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            String sqlSelect = "SELECT tipprecod, tipprenom, tippredes, tippreestreg FROM public.c5m_tipo_predio ORDER BY tipprecod ASC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("tipprecod").trim());
                    fila.add(rs.getString("tipprenom").trim());
                    fila.add(rs.getString("tippredes") != null ? rs.getString("tippredes").trim() : "");
                    
                    String estReg = rs.getString("tippreestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar tipos de predio: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Mapear selección de la fila a las cajas de texto
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtDes.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtCod.setEditable(false); // Restringir edición de clave primaria elegida
                } catch (Exception ex) {
                    // Evitar rupturas en hilos de Swing
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (Entrada manual de PK estructurada de 4 caracteres)
        btnInsertar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El código y la clasificación son campos obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (codigo.length() > 4) {
                JOptionPane.showMessageDialog(this, "El campo Código excede el límite permitido (Máx. 4 caracteres).", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlInsert = "INSERT INTO public.c5m_tipo_predio (tipprecod, tipprenom, tippredes, tippreestreg) VALUES (?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                
                stmt.setString(1, codigo);
                stmt.setString(2, nombre);
                stmt.setString(3, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ ¡Tipo de predio catalogado exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia (Verifique duplicidad en la clave primaria):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un tipo de predio válido para proceder.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlUpdate = "UPDATE public.c5m_tipo_predio SET tipprenom = ?, tippredes = ? WHERE tipprecod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setString(1, nombre);
                stmt.setString(2, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.setString(3, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ Configuración del tipo de predio actualizada de forma conforme.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar el tipo de predio:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> tippreestreg = '0')
        btnDesactivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro activo de la tabla inferior.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Desea cambiar el estado de este tipo de predio a inactivo?\n(Se establecerá tippreestreg = '0')", 
                "Confirmación de Inactivación Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "El tipo de predio seleccionado fue dado de baja del circuito operativo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> tippreestreg = '1')
        btnActivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un tipo de predio inactivo para rehabilitarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ El tipo de predio seleccionado ha sido reactivado y dado de alta con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtNom.setText(""); 
            txtDes.setText(""); 
            txtCod.setEditable(true); // Se restituye la entrada manual de la PK
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso unificado para alternar estados de auditoría (tippreestreg)
    private void cambiarEstadoRegistro(String tipPreCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c5m_tipo_predio SET tippreestreg = ? WHERE tipprecod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, tipPreCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó el identificador de tipo de predio indicado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar estado en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getValorSeguro(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de PostgreSQL no disponible en el entorno de ejecución.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
