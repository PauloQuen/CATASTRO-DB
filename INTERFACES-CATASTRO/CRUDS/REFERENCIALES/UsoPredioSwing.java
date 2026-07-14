package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class UsoPredioSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtNom, txtDes;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public UsoPredioSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Navegación Modular CardLayout) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO DE CAPTURA (Sincronizado de forma exacta con public.c5m_uso_predio)
        JPanel panelFormulario = new JPanel(new GridLayout(3, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Destino y Usos del Predio (c5m_uso_predio) "));

        txtCod = new JTextField();   // usoprecod (VARCHAR(4) - PK Manual)
        txtNom = new JTextField();   // usoprenom (VARCHAR(60))
        txtDes = new JTextField();   // usopredes (VARCHAR(200))

        panelFormulario.add(new JLabel("Código Uso Predio (usoprecod - Máx 4 caracteres):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Nombre del Uso / Destino (usoprenom):"));              panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Descripción Detallada (usopredes):"));               panelFormulario.add(txtDes);

        // 2. BOTONES DE ACCIÓN ADMINISTRATIVA (Gestión de auditoría y estados lógicos)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Uso");
        JButton btnActualizar = new JButton("Modificar Uso");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Paleta semántica estándar de colores para los estados del registro
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

        // 3. TABLA DE REGISTROS INTEGRAL (Visibilidad total para auditoría interna)
        String[] columnas = {"Código Uso (PK)", "Nombre del Uso/Destino", "Descripción Detallada", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA DESDE POSTGRESQL
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            String sqlSelect = "SELECT usoprecod, usoprenom, usopredes, usopreestreg FROM public.c5m_uso_predio ORDER BY usoprecod ASC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("usoprecod").trim());
                    fila.add(rs.getString("usoprenom").trim());
                    fila.add(rs.getString("usopredes") != null ? rs.getString("usopredes").trim() : "");
                    
                    String estReg = rs.getString("usopreestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar el catálogo de usos: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso de selección de la grilla hacia las cajas de texto de edición
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtDes.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtCod.setEditable(false); // Inhabilitar edición del identificador PK en modificaciones
                } catch (Exception ex) {
                    // Evitar interrupciones imprevistas en el hilo de Swing
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (Soporta la inserción manual de la PK VARCHAR(4))
        btnInsertar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El código y el nombre del uso son campos obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (codigo.length() > 4) {
                JOptionPane.showMessageDialog(this, "El campo Código excede el espacio de almacenamiento (Máx. 4 caracteres).", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlInsert = "INSERT INTO public.c5m_uso_predio (usoprecod, usoprenom, usopredes, usopreestreg) VALUES (?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                
                stmt.setString(1, codigo);
                stmt.setString(2, nombre);
                stmt.setString(3, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ ¡Uso de predio registrado con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia (Verifique si el código ingresado ya se encuentra duplicado):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro válido de la tabla inferior para modificar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlUpdate = "UPDATE public.c5m_uso_predio SET usoprenom = ?, usopredes = ? WHERE usoprecod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setString(1, nombre);
                stmt.setString(2, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.setString(3, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ Atributos del uso de predio modificados de forma conforme.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar el uso de predio:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> usopreestreg = '0')
        btnDesactivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro activo desde la grilla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Desea cambiar el estado de este uso de predio a inactivo?\n(Se establecerá usopreestreg = '0')", 
                "Confirmación de Inactivación Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "El uso de predio seleccionado fue dado de baja del circuito operativo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> usopreestreg = '1')
        btnActivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro inactivo para reincorporarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ El uso de predio seleccionado ha sido reactivado y restaurado con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtNom.setText(""); 
            txtDes.setText(""); 
            txtCod.setEditable(true); // Se rehabilita la entrada manual de la PK para nuevos registros
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso unificado para alternar estados de auditoría (usopreestreg)
    private void cambiarEstadoRegistro(String usoPreCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c5m_uso_predio SET usopreestreg = ? WHERE usoprecod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, usoPreCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se localizó el identificador del uso de predio indicado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar el estado en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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