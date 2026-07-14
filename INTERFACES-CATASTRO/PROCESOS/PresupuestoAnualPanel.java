package PROCESOS;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PresupuestoAnualPanel extends JPanel {
    private JTextField txtMuniId, txtAnio, txtMontoEstimado, txtFechaAprobacion;
    private JTextField txtPrecodCierre;
    private JButton btnRegistrarApertura, btnCerrarAnio;
    private JTable tblPresupuestos;
    private DefaultTableModel modelPresupuestos;
    private Connection conexion;

    public PresupuestoAnualPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        conexion = obtenerConexionBD();

        // PANEL SUPERIOR: Formularios de Entrada
        JPanel pnlFormularios = new JPanel();
        pnlFormularios.setLayout(new BoxLayout(pnlFormularios, BoxLayout.Y_AXIS));

        // 1) Formulario Apertura
        JPanel pnlApertura = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlApertura.setBorder(BorderFactory.createTitledBorder(" 1) Apertura de Presupuesto Anual "));
        txtMuniId = new JTextField(4);
        txtAnio = new JTextField(4);
        txtMontoEstimado = new JTextField(8);
        txtFechaAprobacion = new JTextField(8); // AAAA-MM-DD
        btnRegistrarApertura = new JButton("Registrar Apertura");

        pnlApertura.add(new JLabel("Muni ID:")); pnlApertura.add(txtMuniId);
        pnlApertura.add(new JLabel("Año:")); pnlApertura.add(txtAnio);
        pnlApertura.add(new JLabel("Monto S/.:")); pnlApertura.add(txtMontoEstimado);
        pnlApertura.add(new JLabel("Fecha (AAAA-MM-DD):")); pnlApertura.add(txtFechaAprobacion);
        pnlApertura.add(btnRegistrarApertura);

        // 2) Formulario Cierre
        JPanel pnlCierre = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlCierre.setBorder(BorderFactory.createTitledBorder(" 2) Cierre y Auditoría del Ejercicio Fiscal "));
        txtPrecodCierre = new JTextField(6);
        btnCerrarAnio = new JButton("Bloquear y Cerrar Año");
        
        pnlCierre.add(new JLabel("Código Presupuesto (precod):")); pnlCierre.add(txtPrecodCierre);
        pnlCierre.add(btnCerrarAnio);

        pnlFormularios.add(pnlApertura);
        pnlFormularios.add(Box.createVerticalStrut(10));
        pnlFormularios.add(pnlCierre);

        // PANEL CENTRAL: Tabla de Consulta de Presupuestos
        modelPresupuestos = new DefaultTableModel(new String[]{"Código (precod)", "Muni ID", "Año", "Monto Estimado", "Recaudado", "Gastado", "Fecha Aprobación", "Estado Reg."}, 0);
        tblPresupuestos = new JTable(modelPresupuestos);
        JScrollPane scrollPane = new JScrollPane(tblPresupuestos);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Presupuestos Registrados en la Base de Datos "));

        add(pnlFormularios, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // --- EVENTOS ---

        btnRegistrarApertura.addActionListener(e -> {
            if (conexion == null) return;
            if (txtMuniId.getText().trim().isEmpty() || txtAnio.getText().trim().isEmpty() || txtMontoEstimado.getText().trim().isEmpty() || txtFechaAprobacion.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete todos los campos de apertura.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "CALL public.sp_generar_presupuesto_anual(?, ?, ?, ?, ?)";
            try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                cstmt.setInt(1, Integer.parseInt(txtMuniId.getText().trim()));
                cstmt.setShort(2, Short.parseShort(txtAnio.getText().trim()));
                cstmt.setBigDecimal(3, new java.math.BigDecimal(txtMontoEstimado.getText().trim()));
                cstmt.setDate(4, Date.valueOf(txtFechaAprobacion.getText().trim()));
                cstmt.registerOutParameter(5, Types.INTEGER); // p_precod

                cstmt.execute();
                int nuevoPrecod = cstmt.getInt(5);

                JOptionPane.showMessageDialog(this, "¡Presupuesto Registrado!\nSe ha generado el presupuesto con código: " + nuevoPrecod, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                cargarPresupuestos(); // Recargar tabla automáticamente
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al aperturar presupuesto:\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCerrarAnio.addActionListener(e -> {
            if (conexion == null) return;
            if (txtPrecodCierre.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese el código de presupuesto a cerrar.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Implementar la llamada correspondiente para cerrar o auditar el ejercicio
            JOptionPane.showMessageDialog(this, "Ejecutando proceso de cierre para el presupuesto " + txtPrecodCierre.getText());
            cargarPresupuestos();
        });

        // Cargar datos al iniciar la pestaña
        cargarPresupuestos();
    }

    private void cargarPresupuestos() {
        if (conexion == null) return;
        modelPresupuestos.setRowCount(0);
        String sql = "SELECT precod, premun, preanio, premonest, premonpag, premongas, prefecapr, preestreg FROM public.pbm_presupuesto_anual ORDER BY preanio DESC, precod DESC";
        try (Statement stmt = conexion.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelPresupuestos.addRow(new Object[]{
                    rs.getInt("precod"),
                    rs.getInt("premun"),
                    rs.getShort("preanio"),
                    "S/. " + rs.getBigDecimal("premonest"),
                    "S/. " + rs.getBigDecimal("premonpag"),
                    "S/. " + rs.getBigDecimal("premongas"),
                    rs.getDate("prefecapr"),
                    rs.getString("preestreg")
                });
            }
        } catch (SQLException ex) {
            System.out.println("Error al cargar presupuestos: " + ex.getMessage());
        }
    }

    private Connection obtenerConexionBD() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/catastro_municipal", "postgres", "pauloq3408");
        } catch (Exception e) {
            System.out.println("Conexión fallida en PresupuestoAnualPanel: " + e.getMessage());
            return null;
        }
    }
}