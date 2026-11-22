package id.gocrawl.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kumpulan ikon vektor berbasis SVG path Lucide Icons (viewBox 0 0 24 24)
 * yang dirender presisi di Java2D untuk ketajaman penuh pada layar HiDPI.
 */
public final class AcademicIcons {

    private AcademicIcons() {
    }

    public static Icon search(Color color, int size) {
        return lucide(color, size,
                "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z",
                "m21 21-4.3-4.3");
    }

    public static Icon star(Color color, int size) {
        return lucide(color, size,
                "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z");
    }

    public static Icon folder(Color color, int size) {
        return lucide(color, size,
                "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z");
    }

    public static Icon tune(Color color, int size) {
        return lucide(color, size,
                "M20 7h-9", "M14 17H5",
                "M17 17a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
                "M7 7a3 3 0 1 0 0-6 3 3 0 0 0 0 6z");
    }

    public static Icon filter(Color color, int size) {
        return lucide(color, size,
                "M3 6h18", "M7 12h10", "M10 18h4");
    }

    public static Icon filterX(Color color, int size) {
        return lucide(color, size,
                "M13.013 3H2l8 9.46V19l4 2v-8.54l0.9-1.055",
                "m22 3-5 5",
                "m17 3 5 5");
    }

    public static Icon download(Color color, int size) {
        return lucide(color, size,
                "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
                "M7 10l5 5 5-5",
                "M12 15V3");
    }

    public static Icon book(Color color, int size) {
        return lucide(color, size,
                "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20");
    }

    public static Icon person(Color color, int size) {
        return lucide(color, size,
                "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
                "M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z");
    }

    public static Icon building(Color color, int size) {
        return lucide(color, size,
                "M3 22h18", "M6 18v-7", "M10 18v-7", "M14 18v-7", "M18 18v-7",
                "M12 2l9 5H3l9-5z");
    }

    public static Icon badgeCheck(Color color, int size) {
        return lucide(color, size,
                "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
                "m9 12 2 2 4-4");
    }

    public static Icon panelLeft(Color color, int size) {
        return lucide(color, size,
                "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z",
                "M9 3v18");
    }

    public static Icon close(Color color, int size) {
        return lucide(color, size,
                "M18 6 6 18", "m6 6 12 12");
    }

    public static Icon arrowUpRight(Color color, int size) {
        return lucide(color, size,
                "M7 7h10v10", "M7 17 17 7");
    }

    /** Ilustrasi Empty State (dokumen akademik + kaca pembesar + lingkaran putus-putus). */
    public static Icon emptyConstellation(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double scale = size / 64.0;
                g2.scale(scale, scale);

                g2.setColor(new Color(0x25, 0x63, 0xEB, 70));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3.5f, 3.5f}, 0));
                g2.draw(new Ellipse2D.Double(4, 4, 56, 56));

                g2.setColor(AcademicTheme.PRIMARY);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Double(20, 18, 24, 30, 5, 5));
                g2.draw(new Line2D.Double(26, 26, 38, 26));
                g2.draw(new Line2D.Double(26, 31, 38, 31));
                g2.draw(new Line2D.Double(26, 36, 34, 36));

                g2.setColor(AcademicTheme.SURFACE_CONTAINER);
                g2.fill(new Ellipse2D.Double(35, 35, 16, 16));
                g2.setColor(AcademicTheme.PRIMARY);
                g2.draw(new Ellipse2D.Double(35, 35, 16, 16));
                g2.draw(new Line2D.Double(49, 49, 54, 54));

                g2.setColor(AcademicTheme.SECONDARY);
                g2.fill(new Ellipse2D.Double(40, 40, 6, 6));
                g2.dispose();
            }
        };
    }

    private static Icon lucide(Color color, int size, String... svgPaths) {
        List<Path2D> parsed = new ArrayList<>(svgPaths.length);
        for (String d : svgPaths) {
            parsed.add(parseSvgPath(d));
        }
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double scale = size / 24.0;
                g2.scale(scale, scale);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (Path2D p : parsed) {
                    g2.draw(p);
                }
                g2.dispose();
            }
        };
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?");

    private static Path2D parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(d);
        while (m.find()) {
            tokens.add(m.group());
        }

        int i = 0;
        char cmd = ' ';
        char prevCmd = ' ';
        double curX = 0, curY = 0, startX = 0, startY = 0;
        double lastCtrlX = 0, lastCtrlY = 0;
        while (i < tokens.size()) {
            String t = tokens.get(i);
            if (t.length() == 1 && Character.isLetter(t.charAt(0))) {
                cmd = t.charAt(0);
                i++;
                if (cmd == 'Z' || cmd == 'z') {
                    path.closePath();
                    curX = startX;
                    curY = startY;
                    prevCmd = cmd;
                    continue;
                }
            }
            switch (cmd) {
                case 'M' -> {
                    curX = Double.parseDouble(tokens.get(i++));
                    curY = Double.parseDouble(tokens.get(i++));
                    path.moveTo(curX, curY);
                    startX = curX;
                    startY = curY;
                    prevCmd = cmd;
                    cmd = 'L';
                }
                case 'm' -> {
                    curX += Double.parseDouble(tokens.get(i++));
                    curY += Double.parseDouble(tokens.get(i++));
                    path.moveTo(curX, curY);
                    startX = curX;
                    startY = curY;
                    prevCmd = cmd;
                    cmd = 'l';
                }
                case 'L' -> {
                    curX = Double.parseDouble(tokens.get(i++));
                    curY = Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'l' -> {
                    curX += Double.parseDouble(tokens.get(i++));
                    curY += Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'H' -> {
                    curX = Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'h' -> {
                    curX += Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'V' -> {
                    curY = Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'v' -> {
                    curY += Double.parseDouble(tokens.get(i++));
                    path.lineTo(curX, curY);
                    prevCmd = cmd;
                }
                case 'C' -> {
                    double x1 = Double.parseDouble(tokens.get(i++));
                    double y1 = Double.parseDouble(tokens.get(i++));
                    double x2 = Double.parseDouble(tokens.get(i++));
                    double y2 = Double.parseDouble(tokens.get(i++));
                    curX = Double.parseDouble(tokens.get(i++));
                    curY = Double.parseDouble(tokens.get(i++));
                    path.curveTo(x1, y1, x2, y2, curX, curY);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    prevCmd = cmd;
                }
                case 'c' -> {
                    double x1 = curX + Double.parseDouble(tokens.get(i++));
                    double y1 = curY + Double.parseDouble(tokens.get(i++));
                    double x2 = curX + Double.parseDouble(tokens.get(i++));
                    double y2 = curY + Double.parseDouble(tokens.get(i++));
                    curX += Double.parseDouble(tokens.get(i++));
                    curY += Double.parseDouble(tokens.get(i++));
                    path.curveTo(x1, y1, x2, y2, curX, curY);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    prevCmd = cmd;
                }
                case 'S', 's' -> {
                    boolean rel = (cmd == 's');
                    boolean hasPrevCubic = (prevCmd == 'C' || prevCmd == 'c' || prevCmd == 'S' || prevCmd == 's');
                    double x1 = hasPrevCubic ? (2 * curX - lastCtrlX) : curX;
                    double y1 = hasPrevCubic ? (2 * curY - lastCtrlY) : curY;
                    double x2 = Double.parseDouble(tokens.get(i++)) + (rel ? curX : 0);
                    double y2 = Double.parseDouble(tokens.get(i++)) + (rel ? curY : 0);
                    double nx = Double.parseDouble(tokens.get(i++)) + (rel ? curX : 0);
                    double ny = Double.parseDouble(tokens.get(i++)) + (rel ? curY : 0);
                    path.curveTo(x1, y1, x2, y2, nx, ny);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    curX = nx;
                    curY = ny;
                    prevCmd = cmd;
                }
                case 'A', 'a' -> {
                    boolean rel = (cmd == 'a');
                    double rx = Double.parseDouble(tokens.get(i++));
                    double ry = Double.parseDouble(tokens.get(i++));
                    double xAxisRot = Double.parseDouble(tokens.get(i++));
                    boolean largeArc = Double.parseDouble(tokens.get(i++)) != 0;
                    boolean sweep = Double.parseDouble(tokens.get(i++)) != 0;
                    double nx = Double.parseDouble(tokens.get(i++));
                    double ny = Double.parseDouble(tokens.get(i++));
                    if (rel) {
                        nx += curX;
                        ny += curY;
                    }
                    arcTo(path, curX, curY, rx, ry, xAxisRot, largeArc, sweep, nx, ny);
                    curX = nx;
                    curY = ny;
                    prevCmd = cmd;
                }
                default -> i++;
            }
        }
        return path;
    }

    private static void arcTo(Path2D.Double path, double x0, double y0, double rx, double ry,
                              double angleDeg, boolean largeArc, boolean sweep, double x, double y) {
        if (x0 == x && y0 == y) return;
        if (rx == 0 || ry == 0) {
            path.lineTo(x, y);
            return;
        }
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double angle = Math.toRadians(angleDeg % 360.0);
        double cosA = Math.cos(angle), sinA = Math.sin(angle);
        double x1 = cosA * dx2 + sinA * dy2;
        double y1 = -sinA * dx2 + cosA * dy2;
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double Prx = rx * rx, Pry = ry * ry, Px1 = x1 * x1, Py1 = y1 * y1;
        double radiiCheck = Px1 / Prx + Py1 / Pry;
        if (radiiCheck > 1) {
            rx *= Math.sqrt(radiiCheck);
            ry *= Math.sqrt(radiiCheck);
            Prx = rx * rx;
            Pry = ry * ry;
        }
        double sign = (largeArc == sweep) ? -1 : 1;
        double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1)) / ((Prx * Py1) + (Pry * Px1));
        sq = Math.max(0, sq);
        double coef = sign * Math.sqrt(sq);
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);
        double sx2 = (x0 + x) / 2.0;
        double sy2 = (y0 + y) / 2.0;
        double cx = sx2 + (cosA * cx1 - sinA * cy1);
        double cy = sy2 + (sinA * cx1 + cosA * cy1);

        double ux = (x1 - cx1) / rx, uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx, vy = (-y1 - cy1) / ry;
        double theta1 = Math.toDegrees(Math.atan2(uy, ux));
        double dTheta = Math.toDegrees(Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy));
        if (!sweep && dTheta > 0) dTheta -= 360;
        if (sweep && dTheta < 0) dTheta += 360;

        Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, rx * 2, ry * 2, -theta1, -dTheta, Arc2D.OPEN);
        Shape s = (angle != 0) ? AffineTransform.getRotateInstance(angle, cx, cy).createTransformedShape(arc) : arc;
        path.append(s, true);
    }
}
