# Truerize Exam Portal

A secure, production-ready React application for online exam administration with advanced proctoring features.

## 🚀 Features

- **Secure Authentication**: Role-based access control for admins and candidates
- **Real-time Proctoring**: Camera and microphone monitoring during exams
- **Advanced Security**: Anti-cheating measures and session protection
- **Responsive Design**: Mobile-friendly interface with Tailwind CSS
- **Production Ready**: Optimized build, error boundaries, and monitoring

## 🛠️ Tech Stack

- **Frontend**: React 18.2.0 with React Router
- **Styling**: Tailwind CSS with custom components
- **HTTP Client**: Axios with interceptors
- **Face Detection**: face-api.js for proctoring
- **Build Tool**: Create React App with custom optimizations

## 📋 Prerequisites

- Node.js 16.x or higher
- npm or yarn package manager

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd frontend-exam-portal/exam-portal
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Environment Setup**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Start development server**
   ```bash
   npm start
   ```

5. **Build for production**
   ```bash
   npm run build
   ```

## 🔧 Environment Variables

Create a `.env` file based on `.env.example`:

```env
# API Configuration
REACT_APP_API_BASE_URL=https://api.yourdomain.com/api

# Environment
REACT_APP_ENVIRONMENT=production

# Monitoring (Optional)
REACT_APP_SENTRY_DSN=https://your-sentry-dsn@sentry.io/project-id
REACT_APP_LOGGING_ENDPOINT=https://your-logging-endpoint.com/logs
```

## 📁 Project Structure

```
src/
├── api/                 # API configuration and interceptors
├── components/          # Reusable UI components
├── pages/              # Page components (Admin/Candidate)
├── utils/              # Utilities (logger, validation)
├── styles/             # Global styles
└── assets/             # Static assets
```

## 🔒 Security Features

- **Content Security Policy**: Prevents XSS attacks
- **HTTPS Enforcement**: Automatic redirect to secure connections
- **Input Validation**: Comprehensive sanitization
- **Error Boundaries**: Graceful error handling
- **Session Protection**: Anti-tampering measures

## 📊 Monitoring & Logging

- **Production Logging**: Integrated with Sentry/LogRocket
- **Performance Monitoring**: Slow operation detection
- **Error Tracking**: Comprehensive error reporting
- **User Action Logging**: Audit trail for security

## 🧪 Testing

```bash
# Run tests
npm test

# Run tests with coverage
npm test -- --coverage
```

## 🚀 Deployment

### Using Docker
```bash
docker build -t exam-portal .
docker run -p 3000:3000 exam-portal
```

### Using Nginx
```bash
npm run build
# Serve build/ directory with nginx
```

### Environment-Specific Builds
```bash
# Production build
npm run build

# Staging build
REACT_APP_ENVIRONMENT=staging npm run build
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is proprietary software. All rights reserved.

## 🆘 Support

For support, please contact the development team or create an issue in the repository.
