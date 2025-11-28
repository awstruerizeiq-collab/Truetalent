// Input validation utilities for security and data integrity

export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validatePassword = (password) => {
  // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/;
  return passwordRegex.test(password);
};

export const validateUserId = (userId) => {
  // Allow alphanumeric characters, hyphens, and underscores
  const userIdRegex = /^[a-zA-Z0-9_-]+$/;
  return userIdRegex.test(userId) && userId.length >= 3 && userId.length <= 50;
};

export const validateExamId = (examId) => {
  // Allow positive integers
  const num = parseInt(examId, 10);
  return !isNaN(num) && num > 0;
};

export const sanitizeInput = (input) => {
  if (typeof input !== 'string') return input;

  // Remove potentially dangerous characters
  return input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<[^>]*>/g, '')
    .trim();
};

export const validateFileUpload = (file, allowedTypes = [], maxSize = 10 * 1024 * 1024) => {
  if (!file) return { valid: false, error: 'No file provided' };

  if (file.size > maxSize) {
    return { valid: false, error: `File size exceeds ${maxSize / (1024 * 1024)}MB limit` };
  }

  if (allowedTypes.length > 0 && !allowedTypes.includes(file.type)) {
    return { valid: false, error: `File type not allowed. Allowed types: ${allowedTypes.join(', ')}` };
  }

  return { valid: true };
};

export const validateAnswer = (answer, questionType) => {
  if (!answer || answer.trim() === '') {
    return { valid: false, error: 'Answer cannot be empty' };
  }

  switch (questionType) {
    case 'mcq':
      // For MCQ, answer should be a single letter A-D
      if (!/^[A-D]$/.test(answer.toUpperCase())) {
        return { valid: false, error: 'Invalid MCQ answer format' };
      }
      break;

    case 'text':
      // For text answers, basic length validation
      if (answer.length > 10000) {
        return { valid: false, error: 'Answer too long (max 10000 characters)' };
      }
      break;

    default:
      return { valid: false, error: 'Unknown question type' };
  }

  return { valid: true };
};

export const validateSessionData = (data) => {
  const errors = [];

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
