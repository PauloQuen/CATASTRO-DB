package CRUDS.REFERENCIALES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class EscalaTributoSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    private JTextField txtCod, txtVig, txtNom, txtDesc, txtIngMin, txtIngMax, txtPorc, txtMonFij;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    // Bandera para silenciar eventos visuales mientras se limpia o recarga la tabla
    private boolean isAjustando = false; 

    public EscalaTributoSwing(JPanel contenedorPadre, String destinoRetorno) {
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

        // 1. FORMULARIO DE CAPTURA (Sincronizado de forma exacta con public.p9m_escala_tributo)
        JPanel panelFormulario = new JPanel(new GridLayout(4, 4, 10, 10));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Parámetros de Escalas y Alícuotas Tributarias (p9m_escala_tributo) "));

        txtCod = new JTextField();    // esccod (VARCHAR(4) - PK Compuesta)
        txtVig = new JTextField();    // escvig (SMALLINT - PK Compuesta)
        txtNom = new JTextField();    // escnom (VARCHAR(40))
        txtDesc = new JTextField();   // escdesc (VARCHAR(200))
        txtIngMin = new JTextField(); // escingmin (NUMERIC(10,2))
        txtIngMax = new JTextField(); // escingmax (NUMERIC(10,2))
        txtPorc = new JTextField();   // escportrib (NUMERIC(5,2))
        txtMonFij = new JTextField(); // escmonfij (NUMERIC(10,2))

        panelFormulario.add(new JLabel("Código Escala (esccod):"));      panelFormulario.add(txtCod);
        panelFormulario.add(new JLabel("Año Vigencia (escvig):"));       panelFormulario.add(txtVig);
        panelFormulario.add(new JLabel("Nombre Escala (escnom):"));      panelFormulario.add(txtNom);
        panelFormulario.add(new JLabel("Descripción (escdesc):"));       panelFormulario.add(txtDesc);
        panelFormulario.add(new JLabel("Ingreso Mínimo (escingmin):"));  panelFormulario.add(txtIngMin);
        panelFormulario.add(new JLabel("Ingreso Máximo (escingmax):"));  panelFormulario.add(txtIngMax);
        panelFormulario.add(new JLabel("Alícuota / Porcentaje %:"));     panelFormulario.add(txtPorc);
        panelFormulario.add(new JLabel("Monto Fijo Impositivo:"));      panelFormulario.add(txtMonFij);

        // 2. BOTONES DE ACCIÓN ADMINISTRATIVA (Gestión de estados lógicos)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnInsertar = new JButton("Registrar Escala");
        JButton btnActualizar = new JButton("Modificar Escala");
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

        // 3. TABLA DE REGISTROS INTEGRAL (Muestra PK compuesta y estado de auditoría)
        String[] columnas = {"Cod", "Año Vigencia", "Nombre", "Descripción", "Ing. Mínimo", "Ing. Máximo", "Porcentaje %", "Monto Fijo", "Estado Reg."};
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
            String sqlSelect = "SELECT esccod, escvig, escnom, escdesc, escingmin, escingmax, escportrib, escmonfij, escestreg FROM public.p9m_escala_tributo ORDER BY escvig DESC, esccod ASC";
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect)) {
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getString("esccod").trim());
                    fila.add(rs.getShort("escvig"));
                    fila.add(rs.getString("escnom").trim());
                    fila.add(rs.getString("escdesc") != null ? rs.getString("escdesc").trim() : "");
                    fila.add(rs.getBigDecimal("escingmin"));
                    fila.add(rs.getBigDecimal("escingmax"));
                    fila.add(rs.getBigDecimal("escportrib") != null ? rs.getBigDecimal("escportrib") : "");
                    fila.add(rs.getBigDecimal("escmonfij") != null ? rs.getBigDecimal("escmonfij") : "");
                    
                    String estReg = rs.getString("escestreg");
                    fila.add(estReg != null && estReg.equals("1") ? "ACTIVO (1)" : "INACTIVO (0)");
                    
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al sincronizar parámetros de escalas: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // Mapear selección de la grilla hacia las cajas de texto de edición
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                try {
                    txtCod.setText(getValorSeguro(modeloTabla.getValueAt(fila, 0)));
                    txtVig.setText(getValorSeguro(modeloTabla.getValueAt(fila, 1)));
                    txtNom.setText(getValorSeguro(modeloTabla.getValueAt(fila, 2)));
                    txtDesc.setText(getValorSeguro(modeloTabla.getValueAt(fila, 3)));
                    txtIngMin.setText(getValorSeguro(modeloTabla.getValueAt(fila, 4)));
                    txtIngMax.setText(getValorSeguro(modeloTabla.getValueAt(fila, 5)));
                    txtPorc.setText(getValorSeguro(modeloTabla.getValueAt(fila, 6)));
                    txtMonFij.setText(getValorSeguro(modeloTabla.getValueAt(fila, 7)));
                    
                    // Se deshabilitan los dos campos de la PK compuesta para evitar violaciones de clave durante UPDATE
                    txtCod.setEditable(false);
                    txtVig.setEditable(false);
                } catch (Exception ex) {
                    // Prevenir interrupciones del hilo EDT de Swing
                } finally {
                    isAjustando = false;
                }
            }
        });

        // BOTÓN: REGISTRAR (Entrada manual de la PK compuesta)
        btnInsertar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String vigenciaStr = txtVig.getText().trim();
            String nombre = txtNom.getText().trim();
            String ingMinStr = txtIngMin.getText().trim();
            String ingMaxStr = txtIngMax.getText().trim();

            if (codigo.isEmpty() || vigenciaStr.isEmpty() || nombre.isEmpty() || ingMaxStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Los campos Código, Año, Nombre e Ingreso Máximo son mandatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (codigo.length() > 4) {
                JOptionPane.showMessageDialog(this, "El campo Código excede el espacio de almacenamiento permitido (Máx. 4 caracteres).", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlInsert = "INSERT INTO public.p9m_escala_tributo (esccod, escvig, escnom, escdesc, escingmin, escingmax, escportrib, escmonfij, escestreg) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '1')";
            
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                
                stmt.setString(1, codigo);
                stmt.setShort(2, Short.parseShort(vigenciaStr));
                stmt.setString(3, nombre);
                stmt.setString(4, txtDesc.getText().trim().isEmpty() ? null : txtDesc.getText().trim());
                stmt.setBigDecimal(5, ingMinStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(ingMinStr));
                stmt.setBigDecimal(6, new BigDecimal(ingMaxStr));
                
                if (txtPorc.getText().trim().isEmpty()) stmt.setNull(7, Types.NUMERIC);
                else stmt.setBigDecimal(7, new BigDecimal(txtPorc.getText().trim()));
                
                if (txtMonFij.getText().trim().isEmpty()) stmt.setNull(8, Types.NUMERIC);
                else stmt.setBigDecimal(8, new BigDecimal(txtMonFij.getText().trim()));
                
                stmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✔ ¡Escala tributaria incorporada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Por favor compruebe las restricciones de formato numérico decimal o de año entero.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de persistencia (Compruebe duplicidad en la clave compuesta Código + Año):\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: MODIFICAR (Filtrado por clave primaria compuesta)
        btnActualizar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String vigenciaStr = txtVig.getText().trim();
            String nombre = txtNom.getText().trim();
            String ingMinStr = txtIngMin.getText().trim();
            String ingMaxStr = txtIngMax.getText().trim();

            if (codigo.isEmpty() || vigenciaStr.isEmpty() || nombre.isEmpty() || ingMaxStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro válido y complete los atributos obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sqlUpdate = "UPDATE public.p9m_escala_tributo SET escnom = ?, escdesc = ?, escingmin = ?, escingmax = ?, escportrib = ?, escmonfij = ? WHERE esccod = ? AND escvig = ?";
            try (Connection conn = conectar();
                 PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                
                stmt.setString(1, nombre);
                stmt.setString(2, txtDesc.getText().trim().isEmpty() ? null : txtDesc.getText().trim());
                stmt.setBigDecimal(3, ingMinStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(ingMinStr));
                stmt.setBigDecimal(4, new BigDecimal(ingMaxStr));
                
                if (txtPorc.getText().trim().isEmpty()) stmt.setNull(5, Types.NUMERIC);
                else stmt.setBigDecimal(5, new BigDecimal(txtPorc.getText().trim()));
                
                if (txtMonFij.getText().trim().isEmpty()) stmt.setNull(6, Types.NUMERIC);
                else stmt.setBigDecimal(6, new BigDecimal(txtMonFij.getText().trim()));
                
                stmt.setString(7, codigo);
                stmt.setShort(8, Short.parseShort(vigenciaStr));
                
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "✔ Configuración de la escala impositiva actualizada.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al modificar la escala tributaria:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BOTÓN: DESACTIVAR (Baja Lógica -> escestreg = '0')
        btnDesactivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String vigenciaStr = txtVig.getText().trim();
            if (codigo.isEmpty() || vigenciaStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione una escala activa de la grilla inferior.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int conf = JOptionPane.showConfirmDialog(this, 
                "¿Desea cambiar el estado de esta escala impositiva a inactiva?\n(Se establecerá escestreg = '0')", 
                "Confirmación de Inactivación Lógica", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (conf != JOptionPane.YES_OPTION) return;

            cambiarEstadoRegistro(codigo, vigenciaStr, "0", "La escala seleccionada fue dada de baja del circuito activo.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: ACTIVAR (Reactivación Lógica -> escestreg = '1')
        btnActivar.addActionListener(e -> {
            String codigo = txtCod.getText().trim();
            String vigenciaStr = txtVig.getText().trim();
            if (codigo.isEmpty() || vigenciaStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro inactivo de la grilla para rehabilitarlo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cambiarEstadoRegistro(codigo, vigenciaStr, "1", "✔ La escala seleccionada ha sido reactivada con éxito.");
            cargarDatos.run();
            btnLimpiar.doClick();
        });

        // BOTÓN: LIMPIAR
        btnLimpiar.addActionListener(e -> {
            isAjustando = true; 
            txtCod.setText(""); txtVig.setText(""); txtNom.setText(""); txtDesc.setText(""); 
            txtIngMin.setText(""); txtIngMax.setText(""); txtPorc.setText(""); txtMonFij.setText(""); 
            txtCod.setEditable(true); // Se restituyen los controles de la PK compuesta
            txtVig.setEditable(true);
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Proceso unificado para alternar estados de auditoría (escestreg) filtrando por la PK compuesta
    private void cambiarEstadoRegistro(String escCod, String escVig, String nuevoEstado, String mensajeExito) {
        String sqlEstado = "UPDATE public.p9m_escala_tributo SET escestreg = ? WHERE esccod = ? AND escvig = ?";
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlEstado)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setString(2, escCod);
            stmt.setShort(3, Short.parseShort(escVig));
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, mensajeExito);
            } else {
                JOptionPane.showMessageDialog(this, "No se ubicó la escala con los códigos compuestos indicados.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
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
            throw new SQLException("Driver JDBC de PostgreSQL no disponible en el entorno.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}