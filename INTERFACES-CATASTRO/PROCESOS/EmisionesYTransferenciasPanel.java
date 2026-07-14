package PROCESOS;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmisionesYTransferenciasPanel extends JPanel {
    // Parámetros para los Procesos Almacenados (Transacciones)
    private JTextField txtAnioFiscal, txtFecVencimiento;
    private JTextField txtVivCod, txtProNuevo, txtFecTransferencia, txtTituloPropiedad;
    private JButton btnEmitirLote, btnTransferir;
    
    // Filtro ATÓMICO e independiente exclusivo para la Vista/Tabla (Consulta)
    private JTextField txtFiltroAnioTabla;
    private JButton btnLimpiarFiltro;

    private JTable tblTributos;
    private DefaultTableModel modelTributos;
    private Connection conexion;

    public EmisionesYTransferenciasPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        conexion = obtenerConexionBD();

        // ==========================================
        // PANEL SUPERIOR: Formularios de Entrada (Escritura / SPs)
        // ==========================================
        JPanel pnlFormularios = new JPanel();
        pnlFormularios.setLayout(new BoxLayout(pnlFormularios, BoxLayout.Y_AXIS));

        // 3) LOTE ANUAL (SP)
        JPanel pnlLote = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlLote.setBorder(BorderFactory.createTitledBorder(" 3) Emisión Masiva Automatizada de Arbitrios y Tribut "));
        txtAnioFiscal = new JTextField(4);
        txtFecVencimiento = new JTextField(8);
        btnEmitirLote = new JButton("Ejecutar Emisión en Cascada");
        
        pnlLote.add(new JLabel("Año Fiscal:")); pnlLote.add(txtAnioFiscal);
        pnlLote.add(new JLabel("Vencimiento (Opcional):")); pnlLote.add(txtFecVencimiento);
        pnlLote.add(btnEmitirLote);

        // 4) TRANSFERENCIA DOMINIAL (SP)
        JPanel pnlTransferencia = new JPanel(new GridBagLayout());
        pnlTransferencia.setBorder(BorderFactory.createTitledBorder(" 4) Transferencia Dominial de Predios (Compra / Venta / Sucesión) "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); gbc.fill = GridBagConstraints.HORIZONTAL;

        txtVivCod = new JTextField(8); txtProNuevo = new JTextField(5);
        txtFecTransferencia = new JTextField(8); txtTituloPropiedad = new JTextField(12);
        btnTransferir = new JButton("Registrar Mutación de Propiedad");

        gbc.gridx = 0; gbc.gridy = 0; pnlTransferencia.add(new JLabel("Código Predio (vivcod):"), gbc);
        gbc.gridx = 1; pnlTransferencia.add(txtVivCod, gbc);
        gbc.gridx = 2; pnlTransferencia.add(new JLabel("Nuevo Propietario ID:"), gbc);
        gbc.gridx = 3; pnlTransferencia.add(txtProNuevo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; pnlTransferencia.add(new JLabel("Fecha Transferencia:"), gbc);
        gbc.gridx = 1; pnlTransferencia.add(txtFecTransferencia, gbc);
        gbc.gridx = 2; pnlTransferencia.add(new JLabel("Título / Escritura:"), gbc);
        gbc.gridx = 3; pnlTransferencia.add(txtTituloPropiedad, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; pnlTransferencia.add(btnTransferir, gbc);

        pnlFormularios.add(pnlLote);
        pnlFormularios.add(Box.createVerticalStrut(10));
        pnlFormularios.add(pnlTransferencia);

        // ==========================================
        // PANEL CENTRAL: Controles de Filtro + Tabla (Consulta / Lectura)
        // ==========================================
        JPanel pnlConsultaYFiltro = new JPanel(new BorderLayout(5, 5));
        pnlConsultaYFiltro.setBorder(BorderFactory.createTitledBorder(" Estado de Cuentas y Tributos Emitidos (pat_tributo_cab) "));

        // Barra de herramientas de control para la consulta (Filtro Separado)
        JPanel pnlBarraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        txtFiltroAnioTabla = new JTextField(6);
        btnLimpiarFiltro = new JButton("Mostrar Todos");

        pnlBarraFiltro.add(new JLabel("Filtrar vista por Año Fiscal:"));
        pnlBarraFiltro.add(txtFiltroAnioTabla);
        pnlBarraFiltro.add(btnLimpiarFiltro);

        // Tabla
        modelTributos = new DefaultTableModel(new String[]{"Código Cta.", "Propietario ID", "Año Fiscal", "Monto Base Cal.", "Monto Pagado", "Fecha Venc."}, 0);
        tblTributos = new JTable(modelTributos);
        JScrollPane scrollPane = new JScrollPane(tblTributos);

        pnlConsultaYFiltro.add(pnlBarraFiltro, BorderLayout.NORTH);
        pnlConsultaYFiltro.add(scrollPane, BorderLayout.CENTER);

        // Distribución Principal del Panel
        add(pnlFormularios, BorderLayout.NORTH);
        add(pnlConsultaYFiltro, BorderLayout.CENTER);

        // ==========================================
        // ACCIONES Y LOGICA DE NEGOCIO
        // ==========================================

        btnEmitirLote.addActionListener(e -> {
            if (conexion == null) return;
            if (txtAnioFiscal.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar el Año Fiscal para procesar la emisión.", "Faltan Datos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "CALL public.sp_generar_tributos_anuales(?, ?)";
            try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                cstmt.setShort(1, Short.parseShort(txtAnioFiscal.getText().trim()));
                if (txtFecVencimiento.getText().trim().isEmpty()) {
                    cstmt.setNull(2, Types.DATE);
                } else {
                    cstmt.setDate(2, Date.valueOf(txtFecVencimiento.getText().trim()));
                }

                cstmt.execute();
                JOptionPane.showMessageDialog(this, "¡Emisión masiva en cascada procesada con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                // Actualizamos la consulta visual de inmediato tras el cálculo
                cargarTributosEmitidos(); 

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de Base de Datos al emitir lote:\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnTransferir.addActionListener(e -> {
            if (conexion == null) return;
            if (txtVivCod.getText().trim().isEmpty() || txtProNuevo.getText().trim().isEmpty() || 
                txtFecTransferencia.getText().trim().isEmpty() || txtTituloPropiedad.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete todos los campos para registrar la transferencia.", "Faltan Datos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "CALL public.sp_registrar_transferencia_predio(?, ?, ?, ?)";
            try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                cstmt.setString(1, txtVivCod.getText().trim());
                cstmt.setInt(2, Integer.parseInt(txtProNuevo.getText().trim()));
                cstmt.setDate(3, Date.valueOf(txtFecTransferencia.getText().trim()));
                cstmt.setString(4, txtTituloPropiedad.getText().trim());

                cstmt.execute();
                JOptionPane.showMessageDialog(this, "¡Mutación de propiedad registrada!", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                txtVivCod.setText(""); txtProNuevo.setText(""); txtFecTransferencia.setText(""); txtTituloPropiedad.setText("");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al registrar transferencia:\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ESCUCHADOR EXCLUSIVO DEL CAMPO DE FILTRADO (Reactivo e independiente)
        txtFiltroAnioTabla.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { cargarTributosEmitidos(); }
            @Override
            public void removeUpdate(DocumentEvent e) { cargarTributosEmitidos(); }
            @Override
            public void changedUpdate(DocumentEvent e) { cargarTributosEmitidos(); }
        });

        btnLimpiarFiltro.addActionListener(e -> {
            txtFiltroAnioTabla.setText(""); // Al vaciarlo, se dispara el listener y carga todos los registros
        });

        // Carga inicial
        cargarTributosEmitidos();
    }

    // Método de carga adaptado para usar el campo de texto exclusivo de filtro (txtFiltroAnioTabla)
    private void cargarTributosEmitidos() {
        if (conexion == null) return;
        modelTributos.setRowCount(0);

        String anioFiltro = txtFiltroAnioTabla.getText().trim();
        String sql;

        if (!anioFiltro.isEmpty()) {
            sql = "SELECT tricod, tripro, trianio, trimoncal, trimonpag, trifecven " +
                  "FROM public.pat_tributo_cab " +
                  "WHERE trianio = ? " +
                  "ORDER BY tricod DESC";
        } else {
            sql = "SELECT tricod, tripro, trianio, trimoncal, trimonpag, trifecven " +
                  "FROM public.pat_tributo_cab " +
                  "ORDER BY tricod DESC LIMIT 100";
        }

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            if (!anioFiltro.isEmpty()) {
                pstmt.setShort(1, Short.parseShort(anioFiltro));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modelTributos.addRow(new Object[]{
                        rs.getInt("tricod"),
                        rs.getInt("tripro"),
                        rs.getShort("trianio"),
                        "S/. " + rs.getBigDecimal("trimoncal"),
                        "S/. " + rs.getBigDecimal("trimonpag"),
                        rs.getDate("trifecven")
                    });
                }
            }
        } catch (NumberFormatException nfe) {
            // Evita logs o caídas si escriben letras en el filtro visual
        } catch (SQLException ex) {
            System.out.println("Error al cargar tributos: " + ex.getMessage());
        }
    }

    private Connection obtenerConexionBD() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/catastro_municipal", "postgres", "pauloq3408");
        } catch (Exception e) {
            System.out.println("Conexión fallida en EmisionesYTransferenciasPanel: " + e.getMessage());
            return null;
        }
    }
}