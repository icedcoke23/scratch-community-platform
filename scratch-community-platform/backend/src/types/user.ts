export interface User {
  id: string;
  username: string;
  nickname: string;
  avatarUrl?: string;
  bio?: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  status: number;
  points?: number;
  level?: number;
  createdAt: Date;
  updatedAt?: Date;
}

export interface UserProfile {
  id: string;
  username: string;
  nickname: string;
  avatarUrl?: string;
  bio?: string;
  role: User['role'];
  points: number;
  level: number;
  projectCount: number;
  followerCount: number;
  followingCount: number;
  createdAt: Date;
}

export interface LoginDTO {
  username: string;
  password: string;
}

export interface RegisterDTO {
  username: string;
  password: string;
  nickname: string;
  email?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userInfo: User;
}

export interface UpdateUserDTO {
  nickname?: string;
  avatarUrl?: string;
  bio?: string;
}

export interface ChangePasswordDTO {
  oldPassword: string;
  newPassword: string;
}
