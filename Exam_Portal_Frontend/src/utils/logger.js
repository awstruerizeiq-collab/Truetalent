// Production-ready logging utility
class Logger {
  constructor() {
    this.isDevelopment = process.env.NODE_ENV === 'development';
    this.isProduction = process.env.NODE_ENV === 'production';
  }

  // Info level logging
  info(message, ...args) {
    if (this.isDevelopment) {
      console.log(`[INFO] ${message}`, ...args);
    }
    // In production, send to monitoring service
    if (this.isProduction) {
      this.sendToMonitoring('info', message, args);
    }
  }

  // Warning level logging
  warn(message, ...args) {
    if (this.isDevelopment) {
      console.warn(`[WARN] ${message}`, ...args);
    }
    if (this.isProduction) {
      this.sendToMonitoring('warn', message, args);
    }
  }

  // Error level logging
  error(message, error, ...args) {
    if (this.isDevelopment) {
      console.error(`[ERROR] ${message}`, error, ...args);
    }
    if (this.isProduction) {
      this.sendToMonitoring('error', message, { error: error?.message || error, ...args });
    }
  }

  // Debug level logging (development only)
  debug(message, ...args) {
    if (this.isDevelopment) {
      console.debug(`[DEBUG] ${message}`, ...args);
    }
  }

  // Send logs to monitoring service in production
  sendToMonitoring(level, message, data) {
    try {
      const logData = {
        level,
        message,
        data,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        environment: process.env.REACT_APP_ENVIRONMENT || 'production'
      };

      // Production monitoring integration
      if (process.env.REACT_APP_SENTRY_DSN) {
        // Sentry integration (if configured)
        if (window.Sentry) {
          window.Sentry.captureMessage(message, {
            level: level === 'error' ? 'error' : level === 'warn' ? 'warning' : 'info',
            extra: logData
          });
        }
      }

      // Send to custom monitoring endpoint if configured
      if (process.env.REACT_APP_LOGGING_ENDPOINT) {
        fetch(process.env.REACT_APP_LOGGING_ENDPOINT, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(logData)
        }).catch(() => {}); // Silent fail
      }

      // Fallback: store critical errors in localStorage for debugging
      if (level === 'error') {
        const logs = JSON.parse(localStorage.getItem('app_error_logs') || '[]');
        logs.push(logData);
        if (logs.length > 50) logs.shift(); // Keep only last 50 error logs
        localStorage.setItem('app_error_logs', JSON.stringify(logs));
      }

    } catch (err) {
      // Silent fail to prevent logging errors from breaking the app
    }
  }

  // Performance logging
  performance(label, startTime, endTime) {
    const duration = endTime - startTime;
    this.info(`Performance: ${label} took ${duration}ms`);

    if (this.isProduction && duration > 5000) {
      this.warn(`Slow operation detected: ${label} took ${duration}ms`);
    }
  }

  // User action logging
  userAction(action, details = {}) {
    this.info(`User Action: ${action}`, details);
  }
}

const logger = new Logger();
export default logger;
