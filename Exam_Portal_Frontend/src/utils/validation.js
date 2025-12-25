// Input validation utilities for security and data integrity

// ---------------- EMAIL VALIDATION ----------------
export const validateEmail = (email) => {
  if (typeof email !== 'string') return false;
  if (email.length > 254) return false;

  // Safe, linear-time, length-bounded regex (SonarQube compliant)
  const emailRegex =
    /^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,189}\.[A-Za-z]{2,}$/;

  return emailRegex.test(email);
};

// ---------------- PASSWORD VALIDATION ----------------
export const validatePassword = (password) => {
  if (typeof password !== 'string') return false;

  // At least 8 chars, 1 uppercase, 1 lowercase, 1 number
  const passwordRegex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$/;

  return passwordRegex.test(password);
};

// ---------------- USER ID VALIDATION ----------------
export const validateUserId = (userId) => {
  if (typeof userId !== 'string') return false;

  // Alphanumeric + _ -
  const userIdRegex = /^[A-Za-z0-9_-]{3,50}$/;
  return userIdRegex.test(userId);
};

// ---------------- EXAM ID VALIDATION ----------------
export const validateExamId = (examId) => {
  const num = Number(examId);
  return Number.isInteger(num) && num > 0;
};

// ---------------- INPUT SANITIZATION ----------------
// NO REGEX → completely eliminates ReDoS risk
export const sanitizeInput = (input) => {
  if (typeof input !== 'string') return input;

  const parser = new DOMParser();
  const doc = parser.parseFromString(input, 'text/html');

  return (doc.body.textContent || '').trim();
};

// ---------------- FILE UPLOAD VALIDATION ----------------
export const validateFileUpload = (
  file,
  allowedTypes = [],
  maxSize = 10 * 1024 * 1024
) => {
  if (!file) {
    return { valid: false, error: 'No file provided' };
  }

  if (file.size > maxSize) {
    return {
      valid: false,
      error: `File size exceeds ${maxSize / (1024 * 1024)}MB limit`
    };
  }

  if (allowedTypes.length > 0 && !allowedTypes.includes(file.type)) {
    return {
      valid: false,
      error: `File type not allowed. Allowed types: ${allowedTypes.join(', ')}`
    };
  }

  return { valid: true };
};

// ---------------- ANSWER VALIDATION ----------------
export const validateAnswer = (answer, questionType) => {
  if (typeof answer !== 'string' || answer.trim() === '') {
    return { valid: false, error: 'Answer cannot be empty' };
  }

  switch (questionType) {
    case 'mcq':
      // Single option A–D only (safe regex)
      if (!/^[A-D]$/.test(answer.toUpperCase())) {
        return { valid: false, error: 'Invalid MCQ answer format' };
      }
      break;

    case 'text':
      if (answer.length > 10000) {
        return {
          valid: false,
          error: 'Answer too long (max 10000 characters)'
        };
      }
      break;

    default:
      return { valid: false, error: 'Unknown question type' };
  }

  return { valid: true };
};

// ---------------- SESSION DATA VALIDATION ----------------
export const validateSessionData = (data) => {
  const errors = [];

  if (!data || typeof data !== 'object') {
    return { valid: false, errors: ['Invalid session data'] };
  }

  if (!data.userId || !validateUserId(data.userId)) {
    errors.push('Invalid user ID');
  }

  if (!data.examId || !validateExamId(data.examId)) {
    errors.push('Invalid exam ID');
  }

  if (data.answers && typeof data.answers !== 'object') {
    errors.push('Invalid answers format');
  }

  return {
    valid: errors.length === 0,
    errors
  };
};
