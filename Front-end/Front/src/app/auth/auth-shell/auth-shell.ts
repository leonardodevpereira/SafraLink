import { Component, input } from '@angular/core';

@Component({
  selector: 'app-auth-shell',
  standalone: false,
  templateUrl: './auth-shell.html',
  styleUrl: './auth-shell.scss',
})
export class AuthShell {
  readonly titulo = input.required<string>();
  readonly subtitulo = input('');
}
