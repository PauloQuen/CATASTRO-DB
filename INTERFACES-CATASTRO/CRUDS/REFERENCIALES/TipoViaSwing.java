package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TipoViaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtNom;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public TipoViaSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración de Navegación Modular) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. FORMULARIO DE CAPTURA (Mapeado exacto con c3m_via_tipo de PostgreSQL)
        JPanel panelFormulario = new JPanel(new GridLayout(2, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Catálogo de Tipos de Vía Urbana (c3m_via_tipo) "));

        txtCod = new JTextField();   // viatipcod (VARCHAR(4) - PK Manual)
        txtNom = new JTextField();   // viatipnom (VARCHAR(40))

        panelFormulario.add(new JLabel("Código Tipo Vía (viatipcod - Máx 4 caracteres):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Nombre / Descripción (viatipnom):"));               panelFormulario.add(txtNom);

        // 2. BOTONES DE ACCIÓN ADMINISTRATIVA (Inclusión de Activación y Desactivación lógica)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Tipo");
        JButton btnActualizar = new JButton("Modificar Tipo");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Estilos visuales estables de color para estados de auditoría
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

        // 3. TABLA DE REGISTROS INTEGRAL
        String[] columnas = {"Código Tipo (PK)", "Descripción del Tipo de Vía", "Estado Reg."};
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
            String sqlSelect = "SELECT viatipcod, viatipnom, viatipestreg FROM public.c3m_via_tipo ORDER BY viatipcod ASC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    // Al ser VARCHAR, recuperamos explícitamente con getString() y removemos espacios en blanco
                    fila.add(rs.getString("viatipcod").trim());
                    fila.add(rs.getString("viatipnom").trim());
                    
                    String estReg = rs.getString("viatipestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar tipos de vía: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso de selección de la grilla a controles gráficos
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtCod.setEditable(false); // Bloqueado para evitar alteraciones accidentales de PK en UPDATE
                } catch (Exception ex) {
                    // Prevenir problemas de sincronización en la interfaz
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (viatipcod se incluye explícitamente al no ser serial autonumérico)
        btnInsertar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son estrictamente obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (codigo.length() > 4) {
                JOptionPane.showMessageDialog(this, "El campo Código admite un límite máximo de 4 caracteres (Ej: 'AV', 'JR').", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlInsert = "INSERT INTO public.c3m_via_tipo (viatipcod, viatipnom, viatipestreg) VALUES (?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                
                stmt.setString(1, codigo);
                stmt.setString(2, nombre);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ ¡Tipo de vía registrado exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de consistencia (Compruebe si el código ingresado ya existe en la base de datos):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String nombre = txtNom.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro y complete el nombre de la vía antes de modificar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlUpdate = "UPDATE public.c3m_via_tipo SET viatipnom = ? WHERE viatipcod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setString(1, nombre);
                stmt.setString(2, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ Atributos del tipo de vía modificados de forma conforme.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar el tipo de vía:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> viatipestreg = '0')
        btnDesactivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro de la grilla para aplicar la inhabilitación.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de este tipo de vía a inactivo?\n(Se establecerá viatipestreg = '0')", 
                "Confirmación de Inactivación Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "El tipo de vía ha sido dado de baja lógicamente de los listados operativos.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> viatipestreg = '1')
        btnActivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un tipo de vía inactivo desde la grilla para rehabilitarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ El tipo de vía seleccionado ha sido reactivado y restaurado con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtNom.setText(""); 
            txtCod.setEditable(true); // Se rehabilita la entrada de la clave primaria para nuevos registros
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso centralizado para la mutación controlada del estado lógico (viatipestreg) usando String para la PK
    private void cambiarEstadoRegistro(String viaTipCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c3m_via_tipo SET viatipestreg = ? WHERE viatipcod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, viaTipCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se localizó el identificador operativo provisto.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
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