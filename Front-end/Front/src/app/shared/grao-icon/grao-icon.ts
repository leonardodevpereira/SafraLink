import { Component, computed, input } from '@angular/core';
import { TipoGrao } from '../../core/models';

@Component({
  selector: 'app-grao-icon',
  standalone: false,
  template: `
    <span class="selo" [class]="classe()" aria-hidden="true">
      @switch (grao()) {
        @case ('SOJA') {
          <svg viewBox="0 0 40 40">
            <ellipse cx="15" cy="23" rx="8.5" ry="6.5" transform="rotate(-28 15 23)" />
            <ellipse cx="26" cy="16.5" rx="8.5" ry="6.5" transform="rotate(24 26 16.5)" opacity="0.72" />
            <path class="brilho" d="M10.5 21.5c1.2-1.9 3.2-2.7 5-2.3" />
          </svg>
        }
        @case ('MILHO') {
          <svg viewBox="0 0 40 40">
            <path d="M20 5c5.2 3.6 7.6 9.4 7.6 16.2 0 6.2-3.2 11.3-7.6 14.3-4.4-3-7.6-8.1-7.6-14.3C12.4 14.4 14.8 8.6 20 5Z" />
            <path class="brilho" d="M20 10v21M15.5 15.5h9M14.8 21h10.4M15.6 26.5h8.8" />
            <path class="folha" d="M12 34c-3-4.4-4.4-9-3.6-14.2 3 1.8 5 4.6 5.6 8.4M28 34c3-4.4 4.4-9 3.6-14.2-3 1.8-5 4.6-5.6 8.4" />
          </svg>
        }
        @case ('FEIJAO') {
          <svg viewBox="0 0 40 40">
            <path d="M11.5 24.5C9.4 16.8 18 9.6 26.8 13.2c6.4 2.6 4.6 12.6-4.6 15.4-5 1.5-9.5-.6-10.7-4.1Z" />
            <path class="brilho" d="M17 15.5c-2.2 1.4-3.6 3.6-3.7 6" />
          </svg>
        }
      }
    </span>
  `,
  styles: `
    .selo {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 10px;
      flex: 0 0 auto;
    }

    svg {
      width: 30px;
      height: 30px;
      fill: currentColor;
      stroke: none;
    }

    .brilho {
      fill: none;
      stroke: rgba(255, 255, 255, 0.75);
      stroke-width: 1.6;
      stroke-linecap: round;
    }

    .folha {
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: round;
      opacity: 0.55;
    }

    .soja {
      background: #e4f2e7;
      color: #2f7d45;
    }

    .milho {
      background: #fff1cc;
      color: #c98a00;
    }

    .feijao {
      background: #f6e4dc;
      color: #9a4a2e;
    }
  `,
})
export class GraoIcon {
  readonly grao = input.required<TipoGrao>();
  protected readonly classe = computed(() => `selo ${this.grao().toLowerCase()}`);
}
