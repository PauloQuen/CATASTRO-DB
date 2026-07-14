package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class ZonaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtMunFK, txtNom, txtDes, txtTip, txtSup;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public ZonaSwing(JPanel contenedorPadre, String destinoRetorno) {
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

        // 1. FORMULARIO DE CAPTURA (Mapeado exacto con c2m_zona de tu script de PostgreSQL)
        JPanel panelFormulario = new JPanel(new GridLayout(6, 2, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Ficha de Sectorización y Catastro de Zonas (c2m_zona) "));

        txtCod = new JTextField(); txtCod.setEditable(false); // serial (Autoincrementable)
        txtMunFK = new JTextField(); // zonmun (FK)
        txtNom = new JTextField();   // zonnom
        txtDes = new JTextField();   // zondes
        txtTip = new JTextField();   // zontip (character(1))
        txtSup = new JTextField();   // zonsupkm2 (numeric(10,2))

        panelFormulario.add(new JLabel("Código Zona (zoncod - Auto):")); panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Código Municipalidad FK (zonmun):")); panelFormulario.add(txtMunFK);
        panelFormulario.add(new JLabel("Nombre de la Zona / Urb (zonnom):"));  panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Descripción de la Zona (zondes):"));   panelFormulario.add(txtDes);
        panelFormulario.add(new JLabel("Tipo de Zona (zontip - 1 Carácter):")); panelFormulario.add(txtTip);
        panelFormulario.add(new JLabel("Superficie en Km² (zonsupkm2):"));      panelFormulario.add(txtSup);

        // 2. BOTONES DE ACCIÓN ADMINISTRATIVA (Inclusión de Activación y Desactivación lógica)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Zona");
        JButton btnActualizar = new JButton("Modificar Zona");
        JButton btnDesactivar = new JButton("Desactivar (Baja)");
        JButton btnActivar = new JButton("Activar (Reactivar)");
        JButton btnLimpiar = new JButton("Limpiar Campos");

        // Estilos temáticos estables de color para estados lógicos
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

        // 3. TABLA DE REGISTROS INTEGRAL (Muestra todos los estados para auditoría)
        String[] columnas = {"Zoncod", "Zonmun (FK)", "Nombre Zona", "Descripción", "Tipo", "Superficie Km²", "Estado Reg."};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // LÓGICA DE CARGA AUTOMÁTICA DESDE POSTGRESQL
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            String sqlSelect = "SELECT zoncod, zonmun, zonnom, zondes, zontip, zonsupkm2, zonestreg FROM public.c2m_zona ORDER BY zoncod DESC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("zoncod"));
                    fila.add(rs.getInt("zonmun"));
                    fila.add(rs.getString("zonnom"));
                    fila.add(rs.getString("zondes") != null ? rs.getString("zondes") : "");
                    
                    String tipo = rs.getString("zontip");
                    fila.add(tipo != null ? tipo.trim() : "");
                    
                    BigDecimal sup = rs.getBigDecimal("zonsupkm2");
                    fila.add(sup != null ? sup : "");
                    
                    String estReg = rs.getString("zonestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar catálogo de zonas: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Traspaso de selección de grilla a controles gráficos
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtMunFK.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtDes.setText(getValorSeguro(modeloTabla.getValueAt(fila, 3)));
                    txtTip.setText(getValorSeguro(modeloTabla.getValueAt(fila, 4)));
                    txtSup.setText(getValorSeguro(modeloTabla.getValueAt(fila, 5)));
                } catch (Exception ex) {
                    // Evitar interrupciones de foco
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (zoncod se omite de los parámetros por ser de tipo serial autonumérico)
        btnInsertar.addActionListener(e -> {
            int muniFK = validarEntero(txtMunFK.getText(), "Código Municipalidad FK");
            if (muniFK == -1) return;

            String nom = txtNom.getText().trim();
            String tip = txtTip.getText().trim();

            if (nom.isEmpty() || tip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Los campos Nombre de Zona y Tipo de Zona son estrictamente mandatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (tip.length() > 1) {
                JOptionPane.showMessageDialog(this, "El campo Tipo de Zona admite únicamente 1 carácter de longitud.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlInsert = "INSERT INTO public.c2m_zona (zonmun, zonnom, zondes, zontip, zonsupkm2, zonestreg) VALUES (?, ?, ?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                
                stmt.setInt(1, muniFK);
                stmt.setString(2, nom);
                stmt.setString(3, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.setString(4, tip);
                
                if (txtSup.getText().trim().isEmpty()) {
                    stmt.setNull(5, Types.NUMERIC);
                } else {
                    stmt.setBigDecimal(5, new BigDecimal(txtSup.getText().trim()));
                }
                
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ ¡Zona territorial registrada con éxito!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia (Asegúrese de que el código de la Municipalidad FK exista):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR
        btnActualizar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Zona");
            int muniFK = validarEntero(txtMunFK.getText(), "Código Municipalidad FK");
            if (codigo == -1 || muniFK == -1) return;
            
            String nom = txtNom.getText().trim();
            String tip = txtTip.getText().trim();

            if (nom.isEmpty() || tip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre y tipo de zona no pueden quedar vacíos.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (tip.length() > 1) {
                JOptionPane.showMessageDialog(this, "El campo Tipo de Zona admite únicamente 1 carácter.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlUpdate = "UPDATE public.c2m_zona SET zonmun = ?, zonnom = ?, zondes = ?, zontip = ?, zonsupkm2 = ? WHERE zoncod = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setInt(1, muniFK);
                stmt.setString(2, nom);
                stmt.setString(3, txtDes.getText().trim().isEmpty() ? null : txtDes.getText().trim());
                stmt.setString(4, tip);
                
                if (txtSup.getText().trim().isEmpty()) {
                    stmt.setNull(5, Types.NUMERIC);
                } else {
                    stmt.setBigDecimal(5, new BigDecimal(txtSup.getText().trim()));
                }
                
                stmt.setInt(6, codigo);
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ Atributos geográficos de la zona modificados de manera conforme.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar el asiento:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica administrativos -> zonestreg = '0')
        btnDesactivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Zona");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro de la grilla para aplicar la inhabilitación.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de cambiar el estado de esta zona a inactiva?\n(Se establecerá zonestreg = '0')", 
                "Confirmación de Inactivación Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, "0", "La zona territorial ha sido dada de baja lógicamente de los listados activos.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> zonestreg = '1')
        btnActivar.addActionListener(e -> {
            int codigo = validarEntero(txtCod.getText(), "Código Zona");
            if (codigo == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una zona inactiva de la grilla inferior para rehabilitarla.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, "1", "✔ La zona urbana seleccionada ha sido reactivada y dada de alta con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); 
            txtMunFK.setText("");
            txtNom.setText(""); 
            txtDes.setText("");
            txtTip.setText("");
            txtSup.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso centralizado para la mutación atómica del estado lógico (zonestreg)
    private void cambiarEstadoRegistro(int zonCod, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.c2m_zona SET zonestreg = ? WHERE zoncod = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, zonCod);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se localizó el identificador interno provisto.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar estado en el DBMS:\n" + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int validarEntero(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' es mandatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El campo '" + nombreCampo + "' debe corresponder a un formato numérico entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
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
            throw new SQLException("Driver JDBC de PostgreSQL no disponible en el entorno.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}