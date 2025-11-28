// Input validation utilities for security and data integrity
// Input validation utilities for security and data integrity

export const validateEmail = (email) => {
  
  if (typeof email !== "string" || email.length > 254) return false;
  const parts = email.split("@");
  if (parts.length !== 2) return false;

  const [local, domain] = parts;
  if (local.length === 0 || local.length > 64) return false;
  if (domain.length === 0 || domain.length > 253) return false;
  const localRegex = /^[A-Za-z0-9._%+-]+$/;
  const domainRegex = /^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

  return localRegex.test(local) && domainRegex.test(domain);
};

export const sanitizeHtmlInput = (input) => {
  if (typeof input !== "string") return input;

  return input
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;")
    .trim();
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
  return input
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;")
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
